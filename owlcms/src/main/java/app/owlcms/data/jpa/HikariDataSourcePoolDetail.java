package app.owlcms.data.jpa;

import java.lang.reflect.Field;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;

public class HikariDataSourcePoolDetail {
    private final HikariDataSource dataSource;

    public HikariDataSourcePoolDetail(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public HikariPool getHikariPool() {
    	Field field;
		try {
			field = dataSource.getClass().getDeclaredField("pool");
			field.setAccessible(true);
	        HikariPool hikariPool = (HikariPool) field.get(dataSource);
			return hikariPool;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
    }

    public int getActive() {
        try {
            return getHikariPool().getActiveConnections();
        } catch (Exception ex) {
            return -1;
        }
    }

    public int getMax() {
        return dataSource.getMaximumPoolSize();
    }
}