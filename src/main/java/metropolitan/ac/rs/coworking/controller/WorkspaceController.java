package metropolitan.ac.rs.coworking.controller;

import lombok.RequiredArgsConstructor;
import metropolitan.ac.rs.coworking.model.Booking;
import metropolitan.ac.rs.coworking.model.Workspace;
import metropolitan.ac.rs.coworking.model.WorkspaceType;
import metropolitan.ac.rs.coworking.repository.BookingRepository;
import metropolitan.ac.rs.coworking.service.WorkspaceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final BookingRepository bookingRepository;

    @GetMapping
    public ResponseEntity<List<Workspace>> getAllWorkspaces(
            @RequestParam(required = false) WorkspaceType type,
            @RequestParam(required = false) Boolean hasProjector
            ) {
        return ResponseEntity.ok(workspaceService.getAllWorkspaces(type, hasProjector));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Workspace> getWorkspaceById(@PathVariable Long id) {
        return ResponseEntity.ok(workspaceService.getWorkspaceById(id));
    }

    @GetMapping("/{id}/bookings")
    public ResponseEntity<List<Booking>> getBookingsByWorkspace(@PathVariable Long id) {
        return ResponseEntity.ok(bookingRepository.findByWorkspaceId(id));
    }

    @PostMapping
    public ResponseEntity<Workspace> createWorkspace(@RequestBody Workspace workspace) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workspaceService.createWorkspace(workspace));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Workspace> updateWorkspace(@PathVariable Long id, @RequestBody Workspace workspace) {
        return ResponseEntity.ok(workspaceService.updateWorkspace(id, workspace));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkspace(@PathVariable Long id) {
        workspaceService.deleteWorkspace(id);
        return ResponseEntity.noContent().build();
    }

}
