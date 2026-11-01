package com.verify_x.serviceImpl;


import com.verify_x.dto.CandidateProfileDto;
import com.verify_x.dto.CandidateIdentityDto;
import com.verify_x.entity.Candidate;
import com.verify_x.entity.Employment;
import com.verify_x.exception.BadRequestException;
import com.verify_x.repository.CandidateRepository;
import com.verify_x.repository.EmploymentRepository;
import com.verify_x.services.CandidateService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;

import java.util.ArrayList;
import java.util.HashSet;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CandidateServiceImpl implements CandidateService {

    private final CandidateRepository candidateRepository;
    private final EmploymentRepository employmentRepository;

    @Override
    @Transactional
    public CandidateProfileDto getCandidateProfile(Long userId) {

        Candidate candidate = candidateRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("Candidate not found"));

        return CandidateProfileDto.builder()
                .username(candidate.getUsername())
                .email(candidate.getEmail())
                .phoneNumber(candidate.getPhoneNumber())
                .appliedRole(candidate.getAppliedRole())
                .candidateType(candidate.getCandidateType())
                .address(candidate.getAddress())
                .panNumber(candidate.getPanNumber())
                .aadhaarNumber(candidate.getAadhaarNumber())
                .build();
    }
    @Transactional
    @Override
    public CandidateProfileDto saveCandidateProfile(
            Long userId,
            CandidateProfileDto dto) {

        Candidate user = candidateRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        String pan = dto.getPanNumber() == null
                ? null
                : dto.getPanNumber().trim().toUpperCase();

        String aadhaar = dto.getAadhaarNumber() == null
                ? null
                : dto.getAadhaarNumber().replaceAll("\\D", "");

        String phone = dto.getPhoneNumber() == null
                ? null
                : dto.getPhoneNumber().trim();

        // Validate PAN
        if (pan != null && !pan.isBlank()) {
            if (!pan.matches("^[A-Z]{5}[0-9]{4}[A-Z]$")) {
                throw new RuntimeException("Invalid PAN Number");
            }

            if (candidateRepository.existsByPanNumberAndIdNot(
                    pan, userId)) {
                throw new RuntimeException("PAN Number already exists");
            }
        }

        // Validate Aadhaar
        if (aadhaar != null && !aadhaar.isBlank()) {
            if (!aadhaar.matches("^[0-9]{12}$")) {
                throw new RuntimeException(
                        "Aadhaar Number must contain 12 digits");
            }

            if (candidateRepository.existsByAadhaarNumberAndIdNot(
                    aadhaar, userId)) {
                throw new RuntimeException(
                        "Aadhaar Number already exists");
            }
        }

        // Validate phone
        if (phone != null && !phone.isBlank()) {
            if (!phone.matches("^[6-9][0-9]{9}$")) {
                throw new RuntimeException(
                        "Invalid Indian mobile number");
            }

            if (candidateRepository.existsByPhoneNumberAndIdNot(
                    phone, userId)) {
                throw new RuntimeException(
                        "Phone number already exists");
            }
        }

        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(phone);
        user.setAppliedRole(dto.getAppliedRole());
        user.setCandidateType(dto.getCandidateType());

        user.setAddress(dto.getAddress());
        user.setPanNumber(pan);
        user.setAadhaarNumber(aadhaar);

        Candidate saved = candidateRepository.save(user);

        return CandidateProfileDto.builder()
                .username(saved.getUsername())
                .email(saved.getEmail())
                .phoneNumber(saved.getPhoneNumber())
                .address(saved.getAddress())
                .panNumber(saved.getPanNumber())
                .aadhaarNumber(saved.getAadhaarNumber())
                .appliedRole(saved.getAppliedRole())
                .candidateType(saved.getCandidateType())
                .build();
    }

    @Override
    public CandidateProfileDto updateCandidateProfile(Long userId,
                                                      CandidateProfileDto dto) {

        Candidate user = candidateRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Candidate candidate = candidateRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));

        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());
       // user.setAppliedRole(dto.getAppliedRole());
        user.setCandidateType(dto.getCandidateType());

        candidateRepository.save(user);

        candidate.setAddress(dto.getAddress());
        candidate.setPanNumber(dto.getPanNumber());
        candidate.setAadhaarNumber(dto.getAadhaarNumber());

        candidateRepository.save(candidate);

        log.info("Candidate profile updated for User ID : {}", userId);

//        return dto;
        return saveCandidateProfile(userId, dto);
    }

    @Override
    @Transactional
    public CandidateIdentityDto updateCandidateIdentity(
            Long userId,
            CandidateIdentityDto dto) {

        Candidate candidate = candidateRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("Candidate not found"));

        String pan = dto.getPanNumber() == null
                ? null
                : dto.getPanNumber().trim().toUpperCase();

        String aadhaar = dto.getAadhaarNumber() == null
                ? null
                : dto.getAadhaarNumber().replaceAll("\\D", "");

        String uan = dto.getUanNumber() == null
                ? null
                : dto.getUanNumber().replaceAll("\\D", "");

        // PAN validation
        if (pan != null && !pan.isBlank()) {

            if (!pan.matches("^[A-Z]{5}[0-9]{4}[A-Z]$")) {
                throw new BadRequestException("Invalid PAN number");
            }

            if (candidateRepository.existsByPanNumberAndIdNot(
                    pan, userId)) {
                throw new BadRequestException(
                        "PAN number already belongs to another candidate");
            }

            candidate.setPanNumber(pan);
        }

        // Aadhaar validation
        if (aadhaar != null && !aadhaar.isBlank()) {

            if (!aadhaar.matches("^[0-9]{12}$")) {
                throw new BadRequestException(
                        "Aadhaar number must contain 12 digits");
            }

            if (candidateRepository.existsByAadhaarNumberAndIdNot(
                    aadhaar, userId)) {
                throw new BadRequestException(
                        "Aadhaar number already belongs to another candidate");
            }

            candidate.setAadhaarNumber(aadhaar);
        }

        candidateRepository.save(candidate);

        // UAN belongs to Employment entity
        if (uan != null && !uan.isBlank()) {

            if (!uan.matches("^[0-9]{12}$")) {
                throw new BadRequestException("Invalid UAN number");
            }

            Employment employment = employmentRepository
                    .findByCandidate(candidate)
                    .orElseGet(() ->
                            Employment.builder()
                                    .candidate(candidate)
                                    .build());

            employment.setUanNumber(uan);

            employmentRepository.save(employment);
        }

        return CandidateIdentityDto.builder()
                .panNumber(candidate.getPanNumber())
                .aadhaarNumber(candidate.getAadhaarNumber())
                .uanNumber(uan)
                .build();
    }

    @Override
    public void saveCandidateProfile(Candidate candidate) {

        candidateRepository.save(candidate);

        log.info("Candidate profile created for User ID : {}", candidate.getId());
    }

}
