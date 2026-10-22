package com.simplebuilding.modules.simplemodels;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** What the model browser lists: search, base-item filter, sort order and paging (SM21 b/c). No client classes. */
public final class ModelQuery {
    private ModelQuery() {}

    /** No base-item filter. */
    public static final String ALL = "";

    public enum Sort {
        NAME_ASC, NAME_DESC, BASE_ITEM;
        public String key() { return name().toLowerCase(Locale.ROOT); }
    }

    private static final Comparator<ModelDefinition> BY_NAME = Comparator
            .comparing((ModelDefinition d) -> d.match_name().toLowerCase(Locale.ROOT)).thenComparing(ModelDefinition::id);

    public static List<ModelDefinition> view(List<ModelDefinition> models, String query, String baseItem, Sort sort) {
        var order = switch (sort) {
            case NAME_ASC -> BY_NAME;
            case NAME_DESC -> BY_NAME.reversed();
            case BASE_ITEM -> Comparator.comparing(ModelDefinition::base_item).thenComparing(BY_NAME);
        };
        return models.stream().filter(d -> d.matches(query) && (baseItem.equals(ALL) || d.base_item().equals(baseItem))).sorted(order).toList();
    }

    /** The filter choices: {@link #ALL}, then every base item of the catalogue once, sorted by id. */
    public static List<String> baseItems(List<ModelDefinition> models) {
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(ALL), models.stream().map(ModelDefinition::base_item).distinct().sorted()).toList();
    }

    public static int pages(int size, int rows) { return Math.max(1, (size + rows - 1) / rows); }

    /** One wheel notch turns one page: up (positive) goes back, down goes on; clamped to the first and last page. */
    public static int scroll(int page, double wheel, int pages) {
        return wheel == 0 ? page : Math.clamp(page - (int)Math.signum(wheel), 0, pages - 1);
    }
}
