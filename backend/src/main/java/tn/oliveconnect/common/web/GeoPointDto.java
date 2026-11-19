package tn.oliveconnect.common.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import tn.oliveconnect.common.error.ApiException;

/** GeoJSON Point : coordinates = [longitude, latitude] (PAS l'inverse). */
public record GeoPointDto(
        @NotNull @Pattern(regexp = "Point", message = "Doit valoir Point") String type,
        @NotNull @Size(min = 2, max = 2, message = "Doit contenir [longitude, latitude]") List<Double> coordinates) {

    public static GeoPointDto from(GeoJsonPoint p) {
        return p == null ? null : new GeoPointDto("Point", List.of(p.getX(), p.getY()));
    }

    /** Valide les bornes puis convertit ; field = nom du champ pour fieldErrors (ex. "location"). */
    public GeoJsonPoint toGeoJson(String field) {
        Double lon = coordinates.get(0), lat = coordinates.get(1);
        if (lon == null || lat == null || lon < -180 || lon > 180 || lat < -90 || lat > 90) {
            throw ApiException.validation(field + ".coordinates",
                    "Longitude dans [-180, 180] et latitude dans [-90, 90]");
        }
        return new GeoJsonPoint(lon, lat);   // x = longitude, y = latitude
    }
}
