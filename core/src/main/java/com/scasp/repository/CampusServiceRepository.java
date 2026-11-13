package com.scasp.repository;

import com.scasp.model.CampusService;
import java.util.List;

/** Read-only list of campus services with map coordinates (used by the Android client's map feature). */
public interface CampusServiceRepository {
    List<CampusService> findAll();
}
