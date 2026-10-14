package com.civil.platform.module.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.civil.platform.module.entity.Question;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}
