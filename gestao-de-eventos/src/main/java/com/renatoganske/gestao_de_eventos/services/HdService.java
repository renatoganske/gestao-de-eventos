package com.renatoganske.gestao_de_eventos.services;

import com.renatoganske.gestao_de_eventos.dtos.CreateHdDto;
import com.renatoganske.gestao_de_eventos.dtos.HdDto;
import com.renatoganske.gestao_de_eventos.entities.Hd;
import com.renatoganske.gestao_de_eventos.exceptions.HdNotFoundException;
import com.renatoganske.gestao_de_eventos.repositories.HdRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
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
        return hdRepository.save(createHdDto.toEntity()).toResponseDto();
    }

    public List<HdDto> getAllHds() {
        return hdRepository.findAll().stream()
                .map(Hd::toResponseDto)
                .collect(Collectors.toList());
    }

    public HdDto getHdById(UUID id) {
        Optional<Hd> optionalHd = hdRepository.findById(id);
        return optionalHd.map(Hd::toResponseDto)
                .orElseThrow(() -> new HdNotFoundException(id));
    }

    @Transactional
    public HdDto updateHd(UUID id, CreateHdDto createHdDto) {
        Hd hd = hdRepository.findById(id)
                .orElseThrow(() -> new HdNotFoundException(id));

        hd.setName(createHdDto.name());
        hd.setCapacityGb(createHdDto.capacityGb());
        hd.setUsedSpaceGb(createHdDto.usedSpaceGb());
        hd.setPhysicalLocation(createHdDto.physicalLocation());
        hd.setSerialNumber(createHdDto.serialNumber());
        hd.setAcquisitionDate(createHdDto.acquisitionDate());
        hd.setStatus(createHdDto.status());

        Hd updatedHd = hdRepository.save(hd);
        return updatedHd.toResponseDto();
    }

    @Transactional
    public void deleteHd(UUID id) {
        Hd hd = hdRepository.findById(id)
                .orElseThrow(() -> new HdNotFoundException(id));
        hdRepository.delete(hd);
    }

}
