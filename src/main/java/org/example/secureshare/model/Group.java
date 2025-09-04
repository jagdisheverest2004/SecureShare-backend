package org.example.secureshare.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user-groups",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "group_name")
        })
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;

    @Column(name = "group_name", unique = true, nullable = false)
    @Size(min = 5, max = 20)
    private String groupName;

    @Column(name = "owner_id")
    private Long ownerId;

    @ElementCollection
    @CollectionTable(name = "group_members", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "member_id")
    private Set<Long> memberIds = new HashSet<>();
}
