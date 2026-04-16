package com.research.experimentplatform.service;

import com.research.experimentplatform.dto.CreateExperimentRequest;
import com.research.experimentplatform.dto.ExperimentDTO;
import com.research.experimentplatform.dto.UpdateExperimentRequest;
import com.research.experimentplatform.exception.BadRequestException;
import com.research.experimentplatform.exception.ForbiddenException;
import com.research.experimentplatform.model.DesignType;
import com.research.experimentplatform.model.Experiment;
import com.research.experimentplatform.model.ExperimentStatus;
import com.research.experimentplatform.model.User;
import com.research.experimentplatform.model.UserRole;
import com.research.experimentplatform.repository.EnrollmentRepository;
import com.research.experimentplatform.repository.ExperimentRepository;
import com.research.experimentplatform.repository.ResearchTeamRepository;
import com.research.experimentplatform.repository.UserRepository;
import com.research.experimentplatform.security.OwnershipChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExperimentServiceTest {

    @Mock
    private ExperimentRepository experimentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ResearchTeamRepository researchTeamRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private OwnershipChecker ownershipChecker;

    @InjectMocks
    private ExperimentService experimentService;

    private User investigador;

    @BeforeEach
    void setUp() {
        investigador = new User("supa-inv-1", "investigador@test.com", UserRole.RESEARCHER);
        investigador.setId(1L);
    }

    // --- createExperiment ---

    @Test
    void createExperiment_datosCorrectos_devuelveExperimentoEnDraft() {
        CreateExperimentRequest request = new CreateExperimentRequest(
                "Mi experimento", "Descripción", DesignType.PRETEST_POSTTEST,
                null, null, null, null, null);

        when(userRepository.findBySupabaseId("supa-inv-1")).thenReturn(Optional.of(investigador));
        when(experimentRepository.save(any(Experiment.class))).thenAnswer(i -> {
            Experiment e = i.getArgument(0);
            e.setId(1L);
            return e;
        });

        ExperimentDTO result = experimentService.createExperiment(request, "supa-inv-1");

        assertNotNull(result);
        assertEquals("Mi experimento", result.getTitle());
        assertEquals(ExperimentStatus.DRAFT, result.getStatus());
    }

    @Test
    void createExperiment_conTextoDeConsentimiento_loGuardaEnElExperimento() {
        CreateExperimentRequest request = new CreateExperimentRequest(
                "Experimento con contrato", null, DesignType.BETWEEN_SUBJECTS,
                null, null, "Texto del contrato de participación.", null, null);

        when(userRepository.findBySupabaseId("supa-inv-1")).thenReturn(Optional.of(investigador));
        when(experimentRepository.save(any(Experiment.class))).thenAnswer(i -> {
            Experiment e = i.getArgument(0);
            e.setId(1L);
            return e;
        });

        ExperimentDTO result = experimentService.createExperiment(request, "supa-inv-1");

        assertEquals("Texto del contrato de participación.", result.getConsentText());
    }

    // --- updateExperiment ---

    @Test
    void updateExperiment_transicionDraftAActive_funciona() {
        Experiment experimento = new Experiment("Test", null, DesignType.LONGITUDINAL,
                ExperimentStatus.DRAFT, investigador);
        experimento.setId(1L);

        UpdateExperimentRequest request = new UpdateExperimentRequest(
                null, null, null, null, null, ExperimentStatus.ACTIVE, null, null, null);

        when(experimentRepository.findById(1L)).thenReturn(Optional.of(experimento));
        when(ownershipChecker.canModify(experimento, "supa-inv-1")).thenReturn(true);
        when(enrollmentRepository.findByExperimentIdAndStatus(1L, com.research.experimentplatform.model.EnrollmentStatus.PENDING))
                .thenReturn(java.util.List.of());
        when(experimentRepository.save(any(Experiment.class))).thenAnswer(i -> i.getArgument(0));

        ExperimentDTO result = experimentService.updateExperiment(1L, request, "supa-inv-1");

        assertEquals(ExperimentStatus.ACTIVE, result.getStatus());
    }

    @Test
    void updateExperiment_transicionActivaADraft_lanzaBadRequest() {
        Experiment experimento = new Experiment("Test", null, DesignType.LONGITUDINAL,
                ExperimentStatus.ACTIVE, investigador);
        experimento.setId(1L);

        // No se puede volver de ACTIVE a DRAFT
        UpdateExperimentRequest request = new UpdateExperimentRequest(
                null, null, null, null, null, ExperimentStatus.DRAFT, null, null, null);

        when(experimentRepository.findById(1L)).thenReturn(Optional.of(experimento));
        when(ownershipChecker.canModify(experimento, "supa-inv-1")).thenReturn(true);

        assertThrows(BadRequestException.class, () ->
                experimentService.updateExperiment(1L, request, "supa-inv-1"));
    }

    @Test
    void updateExperiment_transicionFinishedAActive_lanzaBadRequest() {
        Experiment experimento = new Experiment("Test", null, DesignType.LONGITUDINAL,
                ExperimentStatus.FINISHED, investigador);
        experimento.setId(1L);

        UpdateExperimentRequest request = new UpdateExperimentRequest(
                null, null, null, null, null, ExperimentStatus.ACTIVE, null, null, null);

        when(experimentRepository.findById(1L)).thenReturn(Optional.of(experimento));
        when(ownershipChecker.canModify(experimento, "supa-inv-1")).thenReturn(true);

        assertThrows(BadRequestException.class, () ->
                experimentService.updateExperiment(1L, request, "supa-inv-1"));
    }

    @Test
    void updateExperiment_usuarioNoEsElDueno_lanzaForbidden() {
        Experiment experimento = new Experiment("Test", null, DesignType.LONGITUDINAL,
                ExperimentStatus.DRAFT, investigador);
        experimento.setId(1L);

        UpdateExperimentRequest request = new UpdateExperimentRequest(
                "Nuevo título", null, null, null, null, null, null, null, null);

        when(experimentRepository.findById(1L)).thenReturn(Optional.of(experimento));
        when(ownershipChecker.canModify(experimento, "supa-otro")).thenReturn(false);

        // "supa-otro" intenta modificar un experimento que no le pertenece
        assertThrows(ForbiddenException.class, () ->
                experimentService.updateExperiment(1L, request, "supa-otro"));
    }

    // --- deleteExperiment ---

    @Test
    void deleteExperiment_usuarioNoEsElDueno_lanzaForbidden() {
        Experiment experimento = new Experiment("Test", null, DesignType.LONGITUDINAL,
                ExperimentStatus.DRAFT, investigador);
        experimento.setId(1L);

        when(experimentRepository.findById(1L)).thenReturn(Optional.of(experimento));
        when(ownershipChecker.canModify(experimento, "supa-otro")).thenReturn(false);

        assertThrows(ForbiddenException.class, () ->
                experimentService.deleteExperiment(1L, "supa-otro"));
    }
}
