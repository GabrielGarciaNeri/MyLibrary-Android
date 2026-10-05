package com.gabriel.mylibrary.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gabriel.mylibrary.model.Category
import java.util.Locale

@Database(entities = [LibraryItem::class, Category::class], version = 2, exportSchema = true)
@TypeConverters(LibraryConverters::class)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun itemDao(): LibraryItemDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        val CREATE_DEFAULTS = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                seedDefaults(db)
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS categories (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, colorArgb INTEGER NOT NULL, createdAt INTEGER NOT NULL, " +
                        "sortPosition INTEGER NOT NULL, defaultKey TEXT, normalizedName TEXT NOT NULL)",
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_categories_normalizedName ON categories (normalizedName)")
                seedDefaults(db)
                db.execSQL(
                    "CREATE TABLE library_items_new (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, " +
                        "author TEXT NOT NULL, description TEXT NOT NULL, categoryId INTEGER NOT NULL, " +
                        "subcategory TEXT NOT NULL, currentProgress INTEGER NOT NULL, totalCount INTEGER NOT NULL, " +
                        "isFavorite INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, " +
                        "coverType TEXT NOT NULL, coverPresetId TEXT, coverFileName TEXT, " +
                        "FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT)",
                )
                val oldCategories = mutableListOf<String>()
                db.query("SELECT DISTINCT category FROM library_items").use { cursor ->
                    while (cursor.moveToNext()) oldCategories += cursor.getString(0)
                }
                oldCategories.forEach { oldCategory ->
                    val categoryId = categoryIdForMigration(db, oldCategory)
                    db.execSQL(
                        "INSERT INTO library_items_new " +
                            "(id, title, author, description, categoryId, subcategory, currentProgress, totalCount, " +
                            "isFavorite, createdAt, updatedAt, coverType, coverPresetId, coverFileName) " +
                            "SELECT id, title, author, description, ?, subcategory, currentProgress, totalCount, " +
                            "isFavorite, createdAt, updatedAt, 'NONE', NULL, NULL FROM library_items WHERE category = ?",
                        arrayOf<Any>(categoryId, oldCategory),
                    )
                }
                db.execSQL("DROP TABLE library_items")
                db.execSQL("ALTER TABLE library_items_new RENAME TO library_items")
                db.execSQL("CREATE INDEX index_library_items_categoryId ON library_items(categoryId)")
            }
        }

        private fun seedDefaults(db: SupportSQLiteDatabase) {
            Category.defaults(System.currentTimeMillis()).forEach { category ->
                db.execSQL(
                    "INSERT INTO categories (id, name, colorArgb, createdAt, sortPosition, defaultKey, normalizedName) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    arrayOf<Any?>(
                        category.id,
                        category.name,
                        category.colorArgb,
                        category.createdAt,
                        category.sortPosition,
                        category.defaultKey,
                        category.normalizedName,
                    ),
                )
            }
        }

        private fun categoryIdForMigration(db: SupportSQLiteDatabase, oldCategory: String): Long {
            val key = oldCategory.trim().uppercase(Locale.ROOT)
            val knownId = when (key) {
                "BOOK" -> 1L
                "MANGA" -> 2L
                "MANHWA" -> 3L
                "MANHUA" -> 4L
                "COMIC" -> 5L
                "UNCATEGORIZED", "" -> Category.UNCATEGORIZED_ID
                else -> null
            }
            if (knownId != null) return knownId
            val name = oldCategory.trim().take(Category.MAX_NAME_LENGTH)
            val normalized = name.lowercase(Locale.ROOT)
            db.query("SELECT id FROM categories WHERE normalizedName = ?", arrayOf(normalized)).use { cursor ->
                if (cursor.moveToFirst()) return cursor.getLong(0)
            }
            db.execSQL(
                "INSERT INTO categories (name, colorArgb, createdAt, sortPosition, defaultKey, normalizedName) " +
                    "VALUES (?, ?, ?, (SELECT COALESCE(MAX(sortPosition), -1) + 1 FROM categories), NULL, ?)",
                arrayOf<Any>(name, 0xFF5C6776L, System.currentTimeMillis(), normalized),
            )
            return db.query("SELECT last_insert_rowid()").use { cursor ->
                check(cursor.moveToFirst())
                cursor.getLong(0)
            }
        }
    }
}
