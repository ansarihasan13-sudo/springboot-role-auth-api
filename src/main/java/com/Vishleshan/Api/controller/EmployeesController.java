package com.Vishleshan.Api.controller;

import com.Vishleshan.Api.dto.AdminDashboardResponse;
import com.Vishleshan.Api.dto.EmployeeStatusRequest;
import com.Vishleshan.Api.entity.Employees;
import com.Vishleshan.Api.service.EmployeesService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
public class EmployeesController {

    private final EmployeesService employeesService;

    public EmployeesController(EmployeesService employeesService) {
        this.employeesService = employeesService;
    }

    // GET: All employees
    @GetMapping
    public ResponseEntity<List<Employees>> getAllEmp() {
        return ResponseEntity.ok(
                employeesService.getAllEmp()
        );
    }


    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard() {

        return ResponseEntity.ok(
                employeesService.getAdminDashboard()
        );
    }


    // GET: Employee by UUID
    @GetMapping("/{id}")
    public ResponseEntity<Employees> getEmpById(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                employeesService.getEmpById(id)
        );
    }

    // POST: Create employee
    @PostMapping
    public ResponseEntity<Employees> createAddEmp(
            @RequestBody Employees employees) {

        Employees savedEmployee =
                employeesService.createAddEmp(employees);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedEmployee);
    }

    // PUT: Update employee details
    @PutMapping("/{id}")
    public ResponseEntity<Employees> updateEmp(
            @PathVariable UUID id,
            @RequestBody Employees employees) {

        return ResponseEntity.ok(
                employeesService.updateEmp(id, employees)
        );
    }

    // PATCH: Update employee active/inactive status
    @PatchMapping("/{id}/status")
    public ResponseEntity<Employees> updateEmployeeStatus(
            @PathVariable UUID id,
            @RequestBody EmployeeStatusRequest request) {

        Employees updatedEmployee =
                employeesService.updateEmployeeStatus(
                        id,
                        request.isActive()
                );

        return ResponseEntity.ok(updatedEmployee);
    }

    // DELETE: Delete employee
    @DeleteMapping("/{id}/soft")
    public ResponseEntity<Void> deleteEmp(
            @PathVariable UUID id) {

        employeesService.deleteEmp(id);

        return ResponseEntity.noContent().build();
    }
    // PERMANENT DELETE: Only soft-deleted employees
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> hardDeleteEmp(
            @PathVariable UUID id) {

        employeesService.hardDeleteEmp(id);

        return ResponseEntity.noContent().build();
    }
}