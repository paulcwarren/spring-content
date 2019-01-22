package org.springframework.content.commons.utils;

import org.springframework.core.convert.support.DefaultConversionService;

public class PlacementServiceImpl extends DefaultConversionService implements PlacementService {

    public PlacementServiceImpl() {
        // Issue #57
        //
        // Remove the FallbackObjectToStringConverter (Object -> String).  This converter can cause issues with Entities
        // with String-arg Constructors.  Because the conversion service considers class hierarchies this converter will
        // match the canConvert(entity.getClass(), String.class) call in getResource(S entity) and be used (incorrectly)
        // to determine the entity's location.  Since there is no way to turn of the hierachy matching we remove this
        // converter instead forcing only matching on the domain object class -> String class.
        this.removeConvertible(Object.class, String.class);
    }
}
