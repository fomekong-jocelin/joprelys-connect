package com.joprelys.backend.spatial.application;

import com.joprelys.backend.spatial.api.BedResponse;
import com.joprelys.backend.spatial.api.RoomResponse;
import com.joprelys.backend.spatial.api.SaveBedRequest;
import com.joprelys.backend.spatial.api.SaveRoomRequest;
import com.joprelys.backend.spatial.api.SaveWardRequest;
import com.joprelys.backend.spatial.api.SpatialConfigurationResponse;
import com.joprelys.backend.spatial.api.WardResponse;
import java.util.UUID;

public interface SpatialConfigurationUseCase {
    SpatialConfigurationResponse getConfiguration(UUID organizationId);
    WardResponse createWard(UUID organizationId, SaveWardRequest request);
    WardResponse updateWard(UUID organizationId, UUID id, SaveWardRequest request);
    void deleteWard(UUID organizationId, UUID id);
    RoomResponse createRoom(UUID organizationId, SaveRoomRequest request);
    RoomResponse updateRoom(UUID organizationId, UUID id, SaveRoomRequest request);
    void deleteRoom(UUID organizationId, UUID id);
    BedResponse createBed(UUID organizationId, SaveBedRequest request);
    BedResponse updateBed(UUID organizationId, UUID id, SaveBedRequest request);
    void deleteBed(UUID organizationId, UUID id);
}
