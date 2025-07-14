package com.escola.admin.repository;

import com.escola.admin.model.entity.Parametro;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ParametroRepository extends CrudRepository<Parametro, Integer> {

    Optional<Parametro> findByChave(String chave);
}