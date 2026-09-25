package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateHdDto;
import com.renatoganske.gestao_de_eventos.dtos.HdDto;
import com.renatoganske.gestao_de_eventos.entities.Hd;
import com.renatoganske.gestao_de_eventos.enums.HdStatus;
import com.renatoganske.gestao_de_eventos.exceptions.HdNotFoundException;
import com.renatoganske.gestao_de_eventos.repositories.HdRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HdServiceTest {

    @Mock
    private HdRepository hdRepository;

    @InjectMocks
    private HdService hdService;

    private Hd hd;
    private CreateHdDto createHdDto;

    @BeforeEach
    void setUp() {
        hd = Hd.builder()
                .id(UUID.randomUUID())
                .name("HD Externo 1")
                .capacityGb(2000)
                .usedSpaceGb(500)
                .physicalLocation("Estante A")
                .serialNumber("SN-12345")
                .acquisitionDate(LocalDate.of(2024, 1, 10))
                .status(HdStatus.ACTIVE)
                .build();

        createHdDto = new CreateHdDto(
                hd.getName(),
                hd.getCapacityGb(),
                hd.getUsedSpaceGb(),
                hd.getPhysicalLocation(),
                hd.getSerialNumber(),
                hd.getAcquisitionDate(),
                hd.getStatus());
    }

    @Test
    void createHd_savesAndReturnsResponseDto() {
        ArgumentCaptor<Hd> captor = ArgumentCaptor.forClass(Hd.class);
        when(hdRepository.save(captor.capture())).thenReturn(hd);

        HdDto result = hdService.createHd(createHdDto);

        assertThat(captor.getValue().getName()).isEqualTo(createHdDto.name());
        assertThat(captor.getValue().getCapacityGb()).isEqualTo(createHdDto.capacityGb());
        assertThat(captor.getValue().getUsedSpaceGb()).isEqualTo(createHdDto.usedSpaceGb());
        assertThat(captor.getValue().getPhysicalLocation()).isEqualTo(createHdDto.physicalLocation());
        assertThat(captor.getValue().getSerialNumber()).isEqualTo(createHdDto.serialNumber());
        assertThat(captor.getValue().getAcquisitionDate()).isEqualTo(createHdDto.acquisitionDate());
        assertThat(captor.getValue().getStatus()).isEqualTo(createHdDto.status());

        assertThat(result.id()).isEqualTo(hd.getId());
        assertThat(result.name()).isEqualTo(hd.getName());
        assertThat(result.capacityGb()).isEqualTo(hd.getCapacityGb());
        assertThat(result.usedSpaceGb()).isEqualTo(hd.getUsedSpaceGb());
        assertThat(result.physicalLocation()).isEqualTo(hd.getPhysicalLocation());
        assertThat(result.serialNumber()).isEqualTo(hd.getSerialNumber());
        assertThat(result.acquisitionDate()).isEqualTo(hd.getAcquisitionDate());
        assertThat(result.status()).isEqualTo(hd.getStatus());
    }

    @Test
    void getAllHds_returnsAllMappedHds() {
        Hd other = Hd.builder()
                .id(UUID.randomUUID())
                .name("HD Externo 2")
                .build();
        when(hdRepository.findAll()).thenReturn(List.of(hd, other));

        List<HdDto> result = hdService.getAllHds();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(HdDto::name)
                .containsExactly(hd.getName(), other.getName());
    }

    @Test
    void getAllHds_returnsEmptyListWhenNoHds() {
        when(hdRepository.findAll()).thenReturn(List.of());

        List<HdDto> result = hdService.getAllHds();

        assertThat(result).isEmpty();
    }

    @Test
    void getHdById_returnsHdWhenFound() {
        when(hdRepository.findById(hd.getId())).thenReturn(Optional.of(hd));

        HdDto result = hdService.getHdById(hd.getId());

        assertThat(result.id()).isEqualTo(hd.getId());
        assertThat(result.name()).isEqualTo(hd.getName());
    }

    @Test
    void getHdById_throwsExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(hdRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hdService.getHdById(id))
                .isInstanceOf(HdNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void updateHd_updatesAndReturnsHdWhenFound() {
        UUID id = hd.getId();
        CreateHdDto updateDto = new CreateHdDto(
                "HD Externo Atualizado", 4000, 1000, "Estante B", "SN-99999",
                LocalDate.of(2025, 3, 20), HdStatus.FULL);
        when(hdRepository.findById(id)).thenReturn(Optional.of(hd));
        when(hdRepository.save(any(Hd.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HdDto result = hdService.updateHd(id, updateDto);

        assertThat(result.name()).isEqualTo("HD Externo Atualizado");
        assertThat(result.capacityGb()).isEqualTo(4000);
        assertThat(result.usedSpaceGb()).isEqualTo(1000);
        assertThat(result.physicalLocation()).isEqualTo("Estante B");
        assertThat(result.serialNumber()).isEqualTo("SN-99999");
        assertThat(result.acquisitionDate()).isEqualTo(LocalDate.of(2025, 3, 20));
        assertThat(result.status()).isEqualTo(HdStatus.FULL);
    }

    @Test
    void updateHd_throwsExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(hdRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hdService.updateHd(id, createHdDto))
                .isInstanceOf(HdNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(hdRepository, never()).save(any());
    }

    @Test
    void deleteHd_deletesWhenFound() {
        UUID id = hd.getId();
        when(hdRepository.findById(id)).thenReturn(Optional.of(hd));

        hdService.deleteHd(id);

        verify(hdRepository, times(1)).delete(hd);
    }

    @Test
    void deleteHd_throwsExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(hdRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hdService.deleteHd(id))
                .isInstanceOf(HdNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(hdRepository, never()).delete(any());
    }

    @Test
    void getHdsNearCapacity_returnsOnlyHdsNearCapacity() {
        Hd nearCapacityHd = Hd.builder()
                .id(UUID.randomUUID())
                .name("HD Externo 2")
                .capacityGb(1000)
                .usedSpaceGb(950)
                .build();
        when(hdRepository.findAll()).thenReturn(List.of(hd, nearCapacityHd));

        List<HdDto> result = hdService.getHdsNearCapacity();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(nearCapacityHd.getId());
    }

    @Test
    void getHdsNearCapacity_returnsEmptyWhenNoneNearCapacity() {
        when(hdRepository.findAll()).thenReturn(List.of(hd));

        List<HdDto> result = hdService.getHdsNearCapacity();

        assertThat(result).isEmpty();
    }
}
