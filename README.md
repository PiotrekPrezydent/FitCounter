# FitHeroRPG - Dokumentacja Techniczna & Analiza Logiczna Kodu

## 1. Wstęp
FitHeroRPG to aplikacja *Exergame* łącząca biometrię z mechanikami RPG. Poniższa dokumentacja stanowi **kompletny opis implementacyjny**, wyjaśniający architekturę, cel istnienia poszczególnych modułów oraz szczegółową logikę sterowania.

---

## 2. Inżynieria Sensorów (Szczegóły Implementacji)

### `SquatDetector.kt`
**Cel Klasy:**
Odpowiada za cyfrowe przetwarzanie sygnału (DSP) z akcelerometru. Jej zadaniem jest odróżnienie celowego ruchu przysiadu od przypadkowych wstrząsów telefonu. Jest implementacją strategii `RepDetector` dla treningu dolnych partii mięśniowych.

*   `onSensorChanged(event)`:
    *   **Krok 1 (Filtracja)**: Surowe dane `event.values` (oś X,Y,Z) są przepuszczane przez filtr dolnoprzepustowy (IIR) ze stałą `ALPHA = 0.15`.
        *   *Logika*: `val = alpha * new + (1-alpha) * old`. To usuwa szum (drgania ręki).
    *   **Krok 2 (Normalizacja)**: Obliczana jest długość wektora (Magnituda) wzorem $\sqrt{x^2+y^2+z^2}$.
    *   **Krok 3 (Maszyna Stanów)**: Instrukcja `when (currentState)` steruje logiką:
        *   `IDLE`: Jeśli magnituda < 8.5 (spadek o ~1.3g), zmienia stan na `DESCENDING` i zapisuje znacznik czasu (`now`).
        *   `DESCENDING`: Jeśli magnituda > 11.5 ORAZ upłynęło > 300ms od początku spadku (eliminacja trzasków), zmienia stan na `ASCENDING` i **wywołuje callback `onRepDetected()`**.
        *   `TIMEOUT`: Jeśli w stanie `DESCENDING` minie > 5 sekund, resetuje do `IDLE` (użytkownik przerwał ćwiczenie w połowie).

### `PushUpDetector.kt`
**Cel Klasy:**
Umożliwia wykorzystanie telefonu jako fizycznego przycisku zliczeniowego bez dotykania ekranu. Wykorzystuje sensor zbliżeniowy, aby wykrywać klatkę piersiową użytkownika przy podłodze.

*   `onSensorChanged(event)`:
    *   Pobiera `distance` z sensora `TYPE_PROXIMITY`.
    *   **Logika binarna**:
        *   Jeśli `distance < maxRange` (ciało blisko): ustawia flagę prywatną `isNear = true`.
        *   Przeciwnie (ciało daleko): sprawdzana jest flaga `isNear`. Jeśli była `true`, następuje **wywołanie `onRepDetected()`** i reset flagi do `false`.
    *   *Cel*: Zliczenie następuje na zboczu narastającym (powrót do góry), co wymusza pełny ruch.

---

## 3. Szczegółowy Przegląd Klas i Metod (Line-by-Line Logic)

### 3.1. `TrainingViewModel.kt`
**Cel Klasy:**
Pełni rolę zarządcy stanu aplikacji (State Holder). Separuje logikę biznesową od widoku (Activity), dzięki czemu gra nie resetuje się przy obrocie ekranu. Koordynuje komunikację między sensorami a bazą danych.

*   `initSensor(context, sensorManager)`:
    1.  Wywołuje `detector?.stop()`, aby bezpiecznie ubić poprzedni sensor (np. przy szybkiej zmianie broni).
    2.  Używa `when (trainingType)` do instancjalizacji nowej klasy: `PushUpDetector`, `SquatDetector` lub `StepDetector`.
    3.  W każdym konstruktorze przekazuje **lambda expression**, która definiuje co zrobić po wykryciu ruchu (np. `{ attackMonster(5) }` dla pompki).
    4.  Jeśli flaga `_isTraining` jest aktywna, natychmiast startuje nowy detektor.

*   `toggleTraining()`:
    1.  Pobiera obecną wartość `_isTraining`.
    2.  Jeśli `true` -> Zatrzymuje sensor (`stop()`), ustawia flagę na `false` i wywołuje `saveSession()`.
    3.  Jeśli `false` -> Zeruje liczniki sesyjne (`resetSessionStats()`), startuje sensor i ustawia flagę na `true`.

*   `attackMonster(baseDamage: Int)`:
    1.  **Buff Check**: Pobiera `System.currentTimeMillis()`. Porównuje z zapisanym `damageBuffExpiry`. Jeśli czas obecny < czas wygaśnięcia -> `damage = baseDamage * 2`.
    2.  **Kalkulacja HP**: Odejmuje obrażenia od `_currentHp`. Używa `.coerceAtLeast(0)`, aby HP nie spadło poniżej zera (anty-bug).
    3.  **Aktualizacja UI**: Wypycha nowe HP do `LiveData`.
    4.  **Zapis (Persystencja)**: Natychmiast zapisuje nowe HP do `SharedPreferences` (klucz `MONSTER_HP`), aby po zabiciu apki stan potwora został zachowany.
    5.  **Warunek Zwycięstwa**: Jeśli `newHp == 0`, wywołuje `onMonsterDefeated()`.

*   `onMonsterDefeated()`:
    1.  Generuje nagrody. Pobiera potwora z `MonsterManager` dla *obecnego* poziomu.
    2.  Pętla **Level-Up**: `while (newXp >= targetXp)`. Pozwala na wbicie wielu poziomów naraz (np. po zabiciu potężnego bossa). Wewnątrz pętli zwiększa `level` i przelicza nowy próg XP (`level * 100`).
    3.  **Respawn**: Generuje potwora dla poziomu `level + 1`. Resetuje HP do pełna (`maxHp`).
    4.  Zapisuje nowy indeks potwora (`MONSTER_INDEX`) do pamięci telefonu.

*   `handleStep()`:
    1.  Wywołuje `incrementReps()`.
    2.  **Logika Pasywnego Dochodu**: Sprawdza `if (reps % 10 == 0)`. Co dziesiąty krok dodaje 1 sztukę złota metodą `addGold(1)`.

*   `buyItem(itemId)`:
    1.  Instrukcja sterująca `when (itemId)` sprawdza typ przedmiotu.
    2.  Sprawdza warunek `currentGold >= price`.
    3.  Jeśli stać: odejmuje złoto (`addGold(-price)`).
    4.  **Logika Czasowa**: Oblicza nowy czas wygaśnięcia buffa. Jeśli buff już trwa, dodaje czas do *końca* trwania. Jeśli nie, dodaje do `currentTimeMillis`.
    5.  Zwraca `true` (sukces) lub `false` (brak środków), co pozwala Activity wyświetlić odpowiedni Toast.

### 3.2. `MainActivity.kt`
**Cel Klasy:**
Stanowi główny punkt wejścia (Entry Point) dla użytkownika. Odpowiada za wyświetlanie interfejsu (UI) i przekazywanie akcji użytkownika (dotknięcia) do ViewModelu. Nie zawiera logiki gry, jedynie logikę prezentacji.

*   `updateWeaponUI(type)`:
    1.  Zmienia kolory obramowania przycisków (`strokeColor`) używając `ColorStateList`. Aktywna broń dostaje kolor zielony (`#76FF03`), reszta biały.
    2.  Podmienia tekst instrukcji (`tvStatus`) w zależności od typu (np. "Place phone on floor" dla pompek).

*   `setupListeners()`:
    *   W listenerze przycisku `btnFight` (Walka): Sprawdza wersję Androida (`Build.VERSION.SDK_INT >= Q`). Jeśli użytkownik chce liczyć kroki, a nie ma uprawnień, wywołuje systemowe okno `requestPermissions` zamiast startować trening.

### 3.3. `MonsterManager.kt`
**Cel Klasy:**
Jest to Singleton (obiekt statyczny), który enkapsuluje logikę "Mistrza Gry". Odpowiada za balans rozgrywki i generowanie przeciwników. Oddziela dane statyczne (szablony potworów) od logiki ich skalowania.

*   `getMonsterByIndex(index, level)`:
    1.  Pobiera szablon (Template) z listy (Slime, Orc, itp.).
    2.  **Algorytm Skalowania**: `Math.pow(1.15, level - 1)`. To kluczowa linijka balansująca grę.
    3.  **Boss Logic**: Używa operatora modulo `level % 10 == 0`. Jeśli reszta z dzielenia to 0 (poziomy 10, 20, 30...), podwaja HP (`hp * 2`) i nadaje prefix "Legendary".

### 3.4. `DatabaseHelper.kt`
**Cel Klasy:**
Zapewnia warstwę trwałości dla danych historycznych. Ukrywa przed resztą aplikacji złożoność zapytań SQL. Zarządza tworzeniem i wersjonowaniem bazy danych SQLite.

*   `addSession(...)`:
    1.  Otwiera bazę do zapisu (`writableDatabase`).
    2.  Najpierw wykonuje `SELECT` aby znaleźć ID typu treningu (np. "PUSH_UP" -> id: 1).
    3.  Składa obiekt `ContentValues` (mapa klucz-wartość).
    4.  Wywołuje `db.insert`. Jest to bezpieczniejsze niż ręczne klejenie stringów SQL (zapobiega SQL Injection).

*   `getAllSessions()`:
    1.  Otwiera bazę do odczytu.
    2.  Wykonuje **Raw Query** z klauzulą `JOIN`.
        ```sql
        SELECT ... FROM sessions s JOIN training_types t ON s.training_id = t._id
        ```
    3.  Iteruje po kursorze (`cursor.moveToNext()`), mapując każdy wiersz tabeli na obiekt Kotlinowy `TrainingSession`.

### 3.5. `HistoryActivity.kt` & `HistoryAdapter.kt`
**Cel Klasy (Activity):**
Prezentuje dane historyczne w formie czytelnej listy. Odpowiada za pobranie danych z bazy i ich przetworzenie (agregację) przed wyświetleniem.
**Cel Klasy (Adapter):**
Wzorzec Adaptera tłumaczy obiekty danych (`DailySummary`) na widoki Androida (`View`). Optymalizuje zużycie pamięci poprzez recykling widoków w `RecyclerView`.

*   `HistoryActivity` -> `onCreate()`:
    1.  Pobiera płaską listę sesji z bazy.
    2.  **Transformacja Danych**: Używa funkcji kolekcji `groupBy`.
        ```kotlin
        allSessions.groupBy { dateFormat.format(it.date) }
        ```
        Tworzy to mapę, gdzie kluczem jest data (np. "Fri, 05 Feb"), a wartością lista treningów z tego dnia.
    3.  Dla każdej grupy oblicza sumy (sumOf) dla pompek, przysiadów i złota, tworząc obiekty `DailySummary`.

*   `HistoryAdapter` -> `showCustomPopup()`:
    1.  W pętli `for (session in dayData.sessions)` tworzy dynamicznie nowe widoki `TextView`.
    2.  Koloruje tekst w zależności od typu, używając instrukcji `when`.
    3.  Dodaje te widoki do kontenera `LinearLayout` wewnątrz ScrollView okna dialogowego. Pozwala to przeglądać nieskończoną ilość wpisów bez tworzenia ciężkiego RecyclerView wewnątrz dialogu.

### 3.6. `BmiActivity.kt`
**Cel Klasy:**
Moduł pomocniczy do monitorowania zdrowia użytkownika. Działa niezależnie od głównej pętli gry (Utility Activity).

*   `calculateAndDisplayBMI()`:
    1.  Pobiera wzrost w cm. Konwertuje na metry: `height / 100.0`.
    2.  Zabezpieczenie przed dzieleniem przez zero: `if (heightM > 0)`.
    3.  Instrukcja `when` sprawdza przedziały (18.5, 25.0, 30.0) i ustawia kolor tła karty (`setCardBackgroundColor`) na odpowiedni (zielony, żółty, czerwony).

---

Autor dokumentacji: Piotr Prezydent

