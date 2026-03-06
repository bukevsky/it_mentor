package com.example.it.mentor.mapper;

import com.example.it.mentor.dto.RegisterResponse;
import com.example.it.mentor.dto.UserInfoResponse;
import com.example.it.mentor.entity.Role;
import com.example.it.mentor.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "roles", expression = "java(rolesToStrings(user.getRoles()))")
    RegisterResponse toRegisterResponse(User user);

    @Mapping(target = "roles", expression = "java(rolesToStrings(user.getRoles()))")
    @Mapping(target = "status", expression = "java(user.getStatus().name())")
    UserInfoResponse toUserInfoResponse(User user);

    default List<String> rolesToStrings(Set<Role> roles) {
        return roles.stream()
                .map(role -> role.getCode().name())
                .toList();
    }
}
