package org.eu.polarexpress.conductor.model;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@SuperBuilder
public class Server extends BaseEntity {
    @NotBlank
    @Setter
    private String snowflakeId;
}
