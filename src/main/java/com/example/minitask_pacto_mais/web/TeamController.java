package com.example.minitask_pacto_mais.web;

import com.example.minitask_pacto_mais.service.TeamService;
import com.example.minitask_pacto_mais.web.dtos.TeamDtos.MemberRequest;
import com.example.minitask_pacto_mais.web.dtos.TeamDtos.TeamRequest;
import com.example.minitask_pacto_mais.web.dtos.TeamDtos.TeamResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    public List<TeamResponse> list() {
        return teamService.list();
    }

    @GetMapping("/{id}")
    public TeamResponse get(@PathVariable UUID id) {
        return teamService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamResponse create(@Valid @RequestBody TeamRequest request) {
        return teamService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TeamResponse update(@PathVariable UUID id, @Valid @RequestBody TeamRequest request) {
        return teamService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        teamService.delete(id);
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasRole('ADMIN')")
    public TeamResponse addMember(@PathVariable UUID id, @Valid @RequestBody MemberRequest request) {
        return teamService.addMember(id, request.userId());
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public TeamResponse removeMember(@PathVariable UUID id, @PathVariable UUID userId) {
        return teamService.removeMember(id, userId);
    }
}
