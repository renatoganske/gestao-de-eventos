package com.renatoganske.gestao_de_eventos.controllers.impl;

import com.renatoganske.gestao_de_eventos.controllers.IHdController;
import com.renatoganske.gestao_de_eventos.dtos.CreateHdDto;
import com.renatoganske.gestao_de_eventos.dtos.HdDto;
import com.renatoganske.gestao_de_eventos.services.HdService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class HdController implements IHdController {

    private final HdService hdService;

    @Override
    public ResponseEntity<List<HdDto>> findAll() {
        return ResponseEntity.ok(hdService.getAllHds());
    }

    @Override
    public ResponseEntity<HdDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(hdService.getHdById(id));
    }

    @Override
    public ResponseEntity<HdDto> create(@RequestBody @Valid CreateHdDto requestDto) {
        HdDto responseDto = hdService.createHd(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @Override
    public ResponseEntity<HdDto> update(@PathVariable UUID id, @RequestBody @Valid CreateHdDto requestDto) {
        return ResponseEntity.ok(hdService.updateHd(id, requestDto));
    }

    @Override
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        hdService.deleteHd(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
