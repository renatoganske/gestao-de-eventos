package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateHdDto;
import com.renatoganske.gestao_de_eventos.dtos.HdDto;
import com.renatoganske.gestao_de_eventos.dtos.OptionDto;
import com.renatoganske.gestao_de_eventos.entities.Hd;
import com.renatoganske.gestao_de_eventos.exceptions.HdNotFoundException;
import com.renatoganske.gestao_de_eventos.policies.HdCapacityPolicy;
import com.renatoganske.gestao_de_eventos.repositories.EventCountProjection;
import com.renatoganske.gestao_de_eventos.repositories.HdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class HdService {

    private final HdRepository hdRepository;

    @Transactional
    public HdDto createHd(CreateHdDto createHdDto) {
        // A just-created HD has no events yet.
        return hdRepository.save(createHdDto.toEntity()).toResponseDto(0);
    }

    @Transactional(readOnly = true)
    public List<HdDto> getAllHds() {
        Map<UUID, Integer> eventCounts = EventCountProjection.toMap(hdRepository.countEventsGroupedByHd());
        return hdRepository.findAll().stream()
                .map(hd -> hd.toResponseDto(EventCountProjection.countOf(eventCounts, hd.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public HdDto getHdById(UUID id) {
        Optional<Hd> optionalHd = hdRepository.findById(id);
        return optionalHd.map(hd -> hd.toResponseDto(countEvents(hd)))
                .orElseThrow(() -> new HdNotFoundException(id));
    }

    @Transactional
    public HdDto updateHd(UUID id, CreateHdDto createHdDto) {
        Hd hd = hdRepository.findById(id)
                .orElseThrow(() -> new HdNotFoundException(id));

        hd.setName(createHdDto.name());
        hd.setCapacityGb(createHdDto.capacityGb());
        hd.setRealCapacityGb(createHdDto.realCapacityGb());
        hd.setUsedSpaceGb(createHdDto.usedSpaceGb());
        hd.setPhysicalLocation(createHdDto.physicalLocation());
        hd.setSerialNumber(createHdDto.serialNumber());
        hd.setAcquisitionDate(createHdDto.acquisitionDate());
        hd.setStatus(createHdDto.status());

        Hd updatedHd = hdRepository.save(hd);
        return updatedHd.toResponseDto(countEvents(updatedHd));
    }

    @Transactional
    public void deleteHd(UUID id) {
        Hd hd = hdRepository.findById(id)
                .orElseThrow(() -> new HdNotFoundException(id));
        hdRepository.delete(hd);
    }

    @Transactional(readOnly = true)
    public List<HdDto> getHdsNearCapacity() {
        Map<UUID, Integer> eventCounts = EventCountProjection.toMap(hdRepository.countEventsGroupedByHd());
        return hdRepository.findAll().stream()
                .filter(HdCapacityPolicy::isNearCapacity)
                .map(hd -> hd.toResponseDto(EventCountProjection.countOf(eventCounts, hd.getId())))
                .collect(Collectors.toList());
    }

    /** Same rule as {@link #getHdsNearCapacity()}, without building the DTOs or counting their events. */
    @Transactional(readOnly = true)
    public int countHdsNearCapacity() {
        return Math.toIntExact(hdRepository.findAll().stream().filter(HdCapacityPolicy::isNearCapacity).count());
    }

    private int countEvents(Hd hd) {
        return Math.toIntExact(hdRepository.countEventsByHdId(hd.getId()));
    }

    @Transactional(readOnly = true)
    public List<OptionDto> getHdOptions() {
        return hdRepository.findOptions();
    }
}
