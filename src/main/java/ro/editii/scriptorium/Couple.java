package ro.editii.scriptorium;

import lombok.*;

@NoArgsConstructor @Getter
@AllArgsConstructor @Builder @EqualsAndHashCode
public class Couple<T, R> {
    T first;
    R second;
}
