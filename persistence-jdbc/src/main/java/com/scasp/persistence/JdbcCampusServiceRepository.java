package com.scasp.persistence;

import com.scasp.model.CampusService;
import com.scasp.repository.CampusServiceRepository;

import javax.sql.DataSource;
import java.util.List;

/** Campus services and their (illustrative) coordinates from the campus_service table. */
public class JdbcCampusServiceRepository extends JdbcSupport implements CampusServiceRepository {
    public JdbcCampusServiceRepository(DataSource ds) { super(ds); }

    @Override public List<CampusService> findAll() {
        return query("SELECT service_id, name, description, latitude, longitude FROM campus_service ORDER BY name",
                rs -> new CampusService(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getBigDecimal(4).doubleValue(), rs.getBigDecimal(5).doubleValue()));
    }
}
