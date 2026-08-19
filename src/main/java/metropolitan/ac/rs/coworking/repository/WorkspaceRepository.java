package metropolitan.ac.rs.coworking.repository;

import metropolitan.ac.rs.coworking.model.BookingStatus;
import metropolitan.ac.rs.coworking.model.Workspace;
import metropolitan.ac.rs.coworking.model.WorkspaceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    List<Workspace> findByTypeAndHasProjector(WorkspaceType type, Boolean hasProjector);
    List<Workspace> findByType(WorkspaceType type);
    List<Workspace> findByHasProjector(Boolean hasProjector);
}
