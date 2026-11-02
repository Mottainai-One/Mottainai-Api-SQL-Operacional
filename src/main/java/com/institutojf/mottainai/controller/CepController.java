package com.institutojf.mottainai.controller;

import com.institutojf.mottainai.controller.swagger.CepControllerApi;
import com.institutojf.mottainai.dto.response.CepResponse;
import com.institutojf.mottainai.service.BrasilApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cep")
@RequiredArgsConstructor
public class CepController implements CepControllerApi {

    private final BrasilApiService brasilApiService;

    @Override
    @GetMapping("/{cep}")
    public ResponseEntity<CepResponse> getCepByZipCode(@PathVariable String cep) {
        CepResponse response = brasilApiService.getCep(cep);
        return ResponseEntity.ok(response);
    }

}
