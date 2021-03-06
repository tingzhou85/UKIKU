package knf.kuma.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import knf.kuma.database.dao.AnimeDAO;
import knf.kuma.database.dao.ChaptersDAO;
import knf.kuma.database.dao.DownloadsDAO;
import knf.kuma.database.dao.ExplorerDAO;
import knf.kuma.database.dao.FavsDAO;
import knf.kuma.database.dao.GenresDAO;
import knf.kuma.database.dao.NotificationDAO;
import knf.kuma.database.dao.QueueDAO;
import knf.kuma.database.dao.RecentsDAO;
import knf.kuma.database.dao.RecordsDAO;
import knf.kuma.database.dao.SeeingDAO;
import knf.kuma.pojos.AnimeObject;
import knf.kuma.pojos.DownloadObject;
import knf.kuma.pojos.ExplorerObject;
import knf.kuma.pojos.FavoriteObject;
import knf.kuma.pojos.GenreStatusObject;
import knf.kuma.pojos.NotificationObj;
import knf.kuma.pojos.QueueObject;
import knf.kuma.pojos.RecentObject;
import knf.kuma.pojos.RecordObject;
import knf.kuma.pojos.SeeingObject;

@Database(entities = {
        RecentObject.class,
        AnimeObject.class,
        FavoriteObject.class,
        AnimeObject.WebInfo.AnimeChapter.class,
        NotificationObj.class,
        DownloadObject.class,
        RecordObject.class,
        SeeingObject.class,
        ExplorerObject.class,
        GenreStatusObject.class,
        QueueObject.class
}, version = 8)
public abstract class CacheDB extends RoomDatabase {
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE `genrestatusobject` (`key` INTEGER NOT NULL, "
                    + "`name` TEXT, `count` INTEGER NOT NULL, PRIMARY KEY(`key`))");
        }
    };
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE `queueobject` (`key` INTEGER, `id` INTEGER NOT NULL,"
                    + "`number` TEXT, `eid` TEXT,`isFile` INTEGER NOT NULL,`link` TEXT,`name` TEXT,`aid` TEXT,`time` INTEGER NOT NULL, PRIMARY KEY (`id`))");
        }
    };
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `queueobject`  ADD COLUMN `uri` TEXT");
        }
    };
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `explorerobject`  ADD COLUMN `aid` TEXT");
        }
    };
    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `downloadobject`  ADD COLUMN `headers` TEXT");
        }
    };
    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `downloadobject`  ADD COLUMN `did` TEXT");
            database.execSQL("ALTER TABLE `downloadobject`  ADD COLUMN `eta` TEXT");
            database.execSQL("ALTER TABLE `downloadobject`  ADD COLUMN `speed` TEXT");
        }
    };
    public static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `downloadobject`  ADD COLUMN `time` INTEGER NOT NULL DEFAULT 0");
        }
    };
    public static CacheDB INSTANCE;

    public static void init(Context context) {
        INSTANCE = Room.databaseBuilder(context, CacheDB.class, "cache-db")
                .allowMainThreadQueries()
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8).build();
    }

    public static CacheDB createAndGet(Context context) {
        return Room.databaseBuilder(context, CacheDB.class, "cache-db")
                .allowMainThreadQueries()
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8).build();
    }

    public abstract RecentsDAO recentsDAO();

    public abstract AnimeDAO animeDAO();

    public abstract FavsDAO favsDAO();

    public abstract ChaptersDAO chaptersDAO();

    public abstract NotificationDAO notificationDAO();

    public abstract DownloadsDAO downloadsDAO();

    public abstract RecordsDAO recordsDAO();

    public abstract SeeingDAO seeingDAO();

    public abstract ExplorerDAO explorerDAO();

    public abstract QueueDAO queueDAO();

    public abstract GenresDAO genresDAO();

}
