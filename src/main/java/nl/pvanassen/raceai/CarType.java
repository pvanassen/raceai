package nl.pvanassen.raceai;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.C;

import static lombok.AccessLevel.PRIVATE;

@Getter
@RequiredArgsConstructor(access = PRIVATE)
public class CarType {

    private final String id;

    private final boolean best;

    public static CarType createNormal(String id) {
        return new CarType(id, false);
    }

    public static CarType createBest(String id) {
        return new CarType(id, true);
    }
}
