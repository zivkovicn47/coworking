package metropolitan.ac.rs.coworking.service;

import lombok.RequiredArgsConstructor;
import metropolitan.ac.rs.coworking.exception.ResourceNotFoundException;
import metropolitan.ac.rs.coworking.model.Workspace;
import metropolitan.ac.rs.coworking.model.WorkspaceType;
import metropolitan.ac.rs.coworking.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkspaceService {
    private final WorkspaceRepository workspaceRepository;

    public List<Workspace> getAllWorkspaces(WorkspaceType type, Boolean hasProjector) {
        if (type != null && hasProjector != null) {
            return workspaceRepository.findByTypeAndHasProjector(type, hasProjector);
        } else if (type != null) {
            return workspaceRepository.findByType(type);
        } else if (hasProjector != null) {
            return workspaceRepository.findByHasProjector(hasProjector);
        }
        return workspaceRepository.findAll();
    }

    public Workspace getWorkspaceById(Long id) {
        return workspaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Radna jedinica sa ID-em " +id+ " ne postoji."));
    }

    public Workspace createWorkspace(Workspace workspace) {
        return workspaceRepository.save(workspace);
    }

    public Workspace updateWorkspace(Long id, Workspace updated) {
        Workspace existing = getWorkspaceById(id);
        existing.setName(updated.getName());
        existing.setType(updated.getType());
        existing.setCapacity(updated.getCapacity());
        existing.setHourlyRate(updated.getHourlyRate());
        existing.setHasProjector(updated.getHasProjector());
        return workspaceRepository.save(existing);
    }

    public void deleteWorkspace(Long id){
        Workspace existing = getWorkspaceById(id);
        workspaceRepository.delete(existing);
    }
}
