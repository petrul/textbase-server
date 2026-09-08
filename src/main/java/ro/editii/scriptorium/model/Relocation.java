package ro.editii.scriptorium.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * table usable for keeping old URLs that changed
 */
@Data
@Entity
@NoArgsConstructor @AllArgsConstructor @Builder
public class Relocation {

    @Id
    String oldPath;

    String newPath;
}
