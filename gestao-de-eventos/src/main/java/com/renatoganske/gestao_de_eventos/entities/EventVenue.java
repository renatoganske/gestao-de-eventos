package com.renatoganske.gestao_de_eventos.entities;

import com.renatoganske.gestao_de_eventos.dtos.EventVenueDto;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity(name = "TB_LOCAL_DO_EVENTO")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class EventVenue {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String name;
    @Column(name = "endereco")
    private String adress;
    @Column(name = "cidade")
    private String city;
    @Column(name = "estado")
    private String state;
    @Column(name = "tipo")
    private String type;

    @OneToMany(mappedBy = "eventVenue")
    private List<Event> events;

    private EventVenueDto toResponseDto() {
        return new EventVenueDto(
                this.id,
                this.name,
                this.adress,
                this.city,
                this.state,
                this.type,
                this.events
        );
    }
}
