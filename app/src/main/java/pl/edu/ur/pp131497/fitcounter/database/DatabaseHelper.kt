package pl.edu.ur.pp131497.fitcounter.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import pl.edu.ur.pp131497.fitcounter.TrainingType

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "FitCounter.db"
        private const val DATABASE_VERSION = 2 //this has to be changed every change in db structure

        const val TABLE_TYPES = "training_types"
        const val COL_TYPE_ID = "_id"
        const val COL_TYPE_NAME = "name"

        const val TABLE_SESSIONS = "sessions"
        const val COL_SESSION_ID = "_id"
        const val COL_FK_TYPE_ID = "training_id"
        const val COL_DATE = "date"
        const val COL_REPS = "reps"
        const val COL_CALORIES = "calories"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTypesTable = """
            CREATE TABLE $TABLE_TYPES (
                $COL_TYPE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TYPE_NAME TEXT UNIQUE
            )
        """.trimIndent()

        val createSessionsTable = """
            CREATE TABLE $TABLE_SESSIONS (
                $COL_SESSION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_FK_TYPE_ID INTEGER,
                $COL_DATE INTEGER,
                $COL_REPS INTEGER,
                $COL_CALORIES REAL, 
                FOREIGN KEY($COL_FK_TYPE_ID) REFERENCES $TABLE_TYPES($COL_TYPE_ID)
            )
        """.trimIndent()

        db.execSQL(createTypesTable)
        db.execSQL(createSessionsTable)

        db.execSQL("INSERT INTO $TABLE_TYPES ($COL_TYPE_NAME) VALUES ('${TrainingType.PUSH_UP}')")
        db.execSQL("INSERT INTO $TABLE_TYPES ($COL_TYPE_NAME) VALUES ('${TrainingType.SQUAT}')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SESSIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TYPES")
        onCreate(db)
    }

    //td change from rawquery to something safe
    fun addSession(type: String, reps: Int, date: Long, calories: Double) {
        val db = writableDatabase

        var typeId: Long = -1
        val cursor = db.rawQuery("SELECT $COL_TYPE_ID FROM $TABLE_TYPES WHERE $COL_TYPE_NAME = ?", arrayOf(type))
        if (cursor.moveToFirst()) {
            typeId = cursor.getLong(0)
        }
        cursor.close()

        if (typeId != -1L) {
            val values = ContentValues().apply {
                put(COL_FK_TYPE_ID, typeId)
                put(COL_DATE, date)
                put(COL_REPS, reps)
                put(COL_CALORIES, calories) // Zapis kalorii
            }
            db.insert(TABLE_SESSIONS, null, values)
        }
        db.close()
    }

    //td change from raw query to something safe
    fun getAllSessions(): List<TrainingSession> {
        val list = ArrayList<TrainingSession>()
        val db = readableDatabase
        val query = """
            SELECT s.$COL_SESSION_ID, s.$COL_DATE, s.$COL_REPS, s.$COL_CALORIES, t.$COL_TYPE_NAME 
            FROM $TABLE_SESSIONS s 
            JOIN $TABLE_TYPES t ON s.$COL_FK_TYPE_ID = t.$COL_TYPE_ID 
            ORDER BY s.$COL_DATE DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(0)
                val date = cursor.getLong(1)
                val reps = cursor.getInt(2)
                val calories = cursor.getDouble(3)
                val typeName = cursor.getString(4)
                list.add(TrainingSession(id, date, typeName, reps, calories))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun clearAllData() {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_SESSIONS")
        db.close()
    }
}