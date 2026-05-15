package com.zhh.taskmanager.mapper;

import com.zhh.taskmanager.Entity.Deck;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DeckMapper {

    @Select("SELECT * FROM deck WHERE user_id = #{userId} ORDER BY created_time DESC")
    List<Deck> findByUserId(Long userId);

    @Insert("INSERT INTO deck (user_id, name, description) VALUES (#{userId}, #{name}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Deck deck);

    @Update("UPDATE deck SET name = #{name}, description = #{description} WHERE id = #{id}")
    void update(Deck deck);

    @Delete("DELETE FROM deck WHERE id = #{id}")
    void deleteById(Integer id);

    @Select("SELECT * FROM deck WHERE id = #{id}")
    Deck findById(Integer id);
}