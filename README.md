# FitHeroRPG - Dokumentacja Techniczna Projektu

## 1. Wstęp
FitHeroRPG to aplikacja mobilna typu *Exergame*, łącząca aktywność fizyczną z mechanikami gier RPG. Celem projektu było stworzenie narzędzia, które poprzez grywalizację motywuje użytkownika do regularnych treningów. Aplikacja wykorzystuje wbudowane w smartfon sensory do automatycznego zliczania powtórzeń ćwiczeń (pompki, przysiady, kroki) i przekłada ten wysiłek na postępy w grze. 

System został zaprojektowany w oparciu o nowoczesne standardy programowania na platformę Android (Kotlin, MVVM), z naciskiem na wydajność i precyzję algorytmów przetwarzania sygnałów.

---

## 2. Analiza Algorytmów Sensorowych (Signal Processing)

Kluczowym aspektem inżynieryjnym projektu jest warstwa **Sensor Fusion**, która odpowiada za przekształcanie surowych danych telemetrycznych (akcelerometria, zbliżenie) na zdarzenia cyfrowe. Poniżej przedstawiono szczegółową analizę zastosowanych algorytmów.

### 2.1. Detekcja Przysiadów (`SquatDetector.kt`)
Moduł wykorzystuje 3-osiowy akcelerometr oraz własną implementację maszyny stanów (Finite State Machine).

#### A. Filtracja Sygnału (Signal Conditioning)
Surowe dane z sensora MEMS charakteryzują się znacznym szumem (jitter). Aby wyizolować trend grawitacyjny, zastosowano cyfrowy **Filtr Dolnoprzepustowy (Low-Pass Filter)** typu IIR (Infinite Impulse Response):

```kotlin
private val ALPHA = 0.15f // Współczynnik wygładzania
// g(t) = α * x(t) + (1-α) * g(t-1)
gravity[0] = ALPHA * event.values[0] + (1 - ALPHA) * gravity[0]
gravity[1] = ALPHA * event.values[1] + (1 - ALPHA) * gravity[1]
gravity[2] = ALPHA * event.values[2] + (1 - ALPHA) * gravity[2]
```
Współczynnik `ALPHA = 0.15` eliminuje piki wysokoczęstotliwościowe, pozostawiając użyteczną składową ruchu.

#### B. Obliczanie Magnitudy Wektora
Algorytm jest niezależny od orientacji urządzenia (device orientation agnostic). Obliczenia bazują na magnitudzie wektora wypadkowego:
$$ |v| = \sqrt{x^2 + y^2 + z^2} $$
Pozwala to na poprawne działanie algorytmu niezależnie od tego, czy telefon znajduje się w kieszeni pionowo, czy poziomo.

#### C. Maszyna Stanów (FSM Logic)
Detekcja opiera się na analizie fluktuacji siły przeciążenia względem grawitacji ziemskiej ($1g \approx 9.81 m/s^2$):
1.  **State: DESCENDING** (Faza ekscentryczna):
    Wykrywana, gdy magnituda spada poniżej progu `THRESHOLD_DOWN = 8.5` (stan bliski nieważkości podczas szybkiego opadania).
    Zabezpieczenie czasowe: `MIN_DESCEND_DURATION` eliminuje false-positive wynikające z wstrząsów.
2.  **State: ASCENDING** (Faza koncentryczna):
    Wykrywana przy wzroście magnitudy powyżej `THRESHOLD_UP = 11.5` (siła reakcji podłoża przy wstawaniu).
3.  **State: IDLE**:
    Reset stanu po wykryciu pełnej sekwencji ruchu.

### 2.2. Detekcja Pompki (`PushUpDetector.kt`)
Zastosowano podejście binarne oparte na czujniku zbliżeniowym (`TYPE_PROXIMITY`).
Algorytm implementuje **Histerezę**, aby zapobiec wielokrotnemu zaliczaniu tego samego powtórzenia przy granicznym zasięgu sensora.
*   Zbocze opadające (wejście w zasięg `< maxRange`) ustawia flagę `isNear`.
*   Zaliczenie powtórzenia następuje wyłącznie przy zboczu narastającym (wyjście z zasięgu), pod warunkiem uprzedniego ustawienia flagi.
Gwarantuje to wymuszenie pełnego zakresu ruchu (Full Range of Motion).

### 2.3. Krokomierz (`StepDetector.kt`)
Moduł wykorzystuje sprzętowe przerwania (Hardware Interrupts) z sensora `TYPE_STEP_DETECTOR`.
W przeciwieństwie do ciągłego odpytywania akcelerometru (polling), rozwiązanie to deleguje obliczenia do koprocesora ruchu (Sensor Hub/SoC). Główny wątek aplikacji otrzymuje jedynie asynchroniczne zdarzenie (`event.values[0] == 1.0f`), co drastycznie redukuje zużycie energii i obciążenie CPU.

---

## 3. Logika Biznesowa i Architektura (Game Mechanics)

### Skalowanie Proceduralne (`MonsterManager`)
System progresji oparty jest na funkcji wykładniczej, zapewniającej nieskończoną skalowalność rozgrywki (Infinite Scaling).
Parametry przeciwnika obliczane są w czasie rzeczywistym:
```kotlin
val growthFactor = Math.pow(1.15, (level - 1).toDouble())
val hp = (40 * template.hpScale * growthFactor).toInt()
```
Stała wzrostu `1.15` (15% per level) wymusza geometryczny przyrost trudności, co obliguje użytkownika do korzystania z systemu ekonomii (sklep) i zwiększania intensywności treningów.

### Zarządzanie Stanem (State Management)
ViewModel pełni rolę "Single Source of Truth". Zastosowano `MutableLiveData` do atomowych aktualizacji stanu UI.
Mechanika Buffów operuje na timestampach (`epoch time`), co czyni ją odporną na restarty procesu aplikacji (process death resilience). Stan aktywności buffa jest determinowany przez porównanie `System.currentTimeMillis() < expiryTime`.

---

## 4. Warstwa Danych (Persistence Layer)

Projekt stosuje hybrydowy model persystencji danych:
1.  **SharedPreferences**: Przechowywanie tzw. "hot state" (poziom, złoto, aktualne HP potwora). Wykorzystanie `apply()` zapewnia asynchroniczny zapis bez blokowania wątku UI (ANR prevention).
2.  **SQLite Database**: Przechowywanie ustrukturyzowanej historii sesji treningowych.
    *   Schemat bazy jest **znormalizowany** (3NF) – tabela `sessions` referuje do tabeli słownikowej `training_types` poprzez Klucz Obcy (Foreign Key), co optymalizuje rozmiar bazy i spójność danych.

---

## 5. Podsumowanie Techniczne
Projekt FitHeroRPG demonstruje zaawansowane wykorzystanie API Androida w zakresie:
*   **Hardware Abstraction Layer**: Bezpośrednia komunikacja z sensorami MEMS.
*   **Architektura**: Czysta implementacja MVVM z separacją warstw.
*   **Algorytmika**: Autorskie implementacje filtrów cyfrowych i maszyn stanów.

Autor: [Twoje Imię]
Data: 2026-02-05
