package com.escola.admin.repository;

import com.escola.admin.model.entity.Empresa;
import com.escola.admin.model.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Finds a user by their username. This method is essential for the
     * UserDetailsService to load the user during the authentication process.
     *
     * @param username the username to search for.
     * @return an Optional containing the user if found, or an empty Optional otherwise.
     */
    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByEmail(String email);

    @Query("SELECT e FROM Usuario e " +
            " WHERE (:criteria IS NULL OR :criteria = '') OR " +
            " (LOWER(e.username) LIKE LOWER(CONCAT('%', :criteria, '%')) ) OR " +
            " (LOWER(e.firstname) LIKE LOWER(CONCAT('%', :criteria, '%')) ) OR " +
            " (LOWER(e.email) LIKE LOWER(CONCAT('%', :criteria, '%')) ) OR " +
            " (LOWER(e.lastname) LIKE LOWER(CONCAT('%', :criteria, '%')) ) ")
    Optional<Page<Usuario>> findByFiltro(@Param("criteria") String filtro, Pageable pageable);

    @Query("SELECT e FROM Usuario e " +
            " WHERE e.empresa.id = :idEmpresa " + // This condition always applies
            " AND ( (:criteria IS NULL OR :criteria = '') OR " + // If criteria is null/empty, this is TRUE
            "        (LOWER(e.username) LIKE LOWER(CONCAT('%', :criteria, '%'))) OR " +
            "        (LOWER(e.firstname) LIKE LOWER(CONCAT('%', :criteria, '%'))) OR " +
            "        (LOWER(e.email) LIKE LOWER(CONCAT('%', :criteria, '%'))) OR " +
            "        (LOWER(e.lastname) LIKE LOWER(CONCAT('%', :criteria, '%'))) )" // The OR group for criteria
    )
    Optional<Page<Usuario>> findByFiltroAndEmpresa(@Param("criteria") String filtro, @Param("idEmpresa") Long idEmpresa, Pageable pageable);

    /**
     * Busca diretamente a entidade Empresa associada a um usuário,
     * evitando LazyInitializationException.
     *
     * @param usuarioId O ID do usuário.
     * @return um Optional contendo a Empresa, se existir.
     */
    @Query("SELECT u.empresa FROM Usuario u WHERE u.id = :usuarioId")
    Optional<Empresa> findEmpresaByUsuarioId(@Param("usuarioId") Long usuarioId);
}