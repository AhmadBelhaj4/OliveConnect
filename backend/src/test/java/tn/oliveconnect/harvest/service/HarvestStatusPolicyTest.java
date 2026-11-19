package tn.oliveconnect.harvest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tn.oliveconnect.common.enums.HarvestStatus;
import tn.oliveconnect.common.error.ApiException;
import tn.oliveconnect.common.error.ErrorCode;

/** Test unitaire pur (pas de Spring, pas de base) : 9 transitions autorisées, 40 interdites. */
class HarvestStatusPolicyTest {

    private final HarvestStatusPolicy policy = new HarvestStatusPolicy();

    private static final Set<String> ALLOWED = Set.of(
            "SUBMITTED>APPROVED", "SUBMITTED>REJECTED", "SUBMITTED>CANCELLED",
            "APPROVED>SCHEDULED", "APPROVED>CANCELLED",
            "SCHEDULED>IN_PROGRESS", "SCHEDULED>APPROVED", "SCHEDULED>CANCELLED",
            "IN_PROGRESS>COMPLETED");

    static Stream<Arguments> allowed() {
        return pairs().filter(a -> ALLOWED.contains(key(a)));
    }

    static Stream<Arguments> forbidden() {
        return pairs().filter(a -> !ALLOWED.contains(key(a)));
    }

    @ParameterizedTest(name = "{0} -> {1} autorisée")
    @MethodSource("allowed")
    void allowedTransition(HarvestStatus from, HarvestStatus to) {
        assertThat(policy.canTransition(from, to)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} interdite (409)")
    @MethodSource("forbidden")
    void forbiddenTransition(HarvestStatus from, HarvestStatus to) {
        assertThatThrownBy(() -> policy.assertTransition(from, to))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(ErrorCode.INVALID_STATE_TRANSITION));
    }

    private static Stream<Arguments> pairs() {
        return Stream.of(HarvestStatus.values())
                .flatMap(from -> Stream.of(HarvestStatus.values()).map(to -> Arguments.of(from, to)));
    }

    private static String key(Arguments a) {
        return a.get()[0] + ">" + a.get()[1];
    }
}
