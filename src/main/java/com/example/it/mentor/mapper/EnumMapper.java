package com.example.it.mentor.mapper;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EnumMapper {

    default String enumToString(Enum<?> e) {
        return e != null ? e.name() : null;
    }
}
