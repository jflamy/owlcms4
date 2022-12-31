/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.category;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Key for Association class between Athlete and Category.
 *
 * @author Jean-François Lamy
 */
@Embeddable
public class ParticipationId
        implements Serializable {

    private static final long serialVersionUID = -5619538756170067634L;

    @Column(name = "athlete_id")
    public Long athleteId;

    @Column(name = "category_id")
    public Long categoryId;

    public ParticipationId(Long athleteId, Long categoryId) {
        this();
        this.athleteId = athleteId;
        this.categoryId = categoryId;
    }

    private ParticipationId() {
    }

    // Getters omitted for brevity

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ParticipationId that = (ParticipationId) o;
        return Objects.equals(athleteId, that.athleteId) &&
                Objects.equals(categoryId, that.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(athleteId, categoryId);
    }

}
