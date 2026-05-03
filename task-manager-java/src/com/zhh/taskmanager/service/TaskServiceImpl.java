package com.zhh.taskmanager.service;

import com.zhh.taskmanager.model.Task;
import com.zhh.taskmanager.mapper.TaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskMapper taskMapper;

    @Override
    public void addTask(Task task) {
        task.setCompleted(false);
        taskMapper.insert(task);
        System.out.println("任务已保存: " + task.getTitle());
    }

    @Override
    public List<Task> getTasksByUserId(Long userId) {
        return taskMapper.findByUserId(userId);
    }

    @Override
    public void completeTask(int id, Long userId) {
        taskMapper.toggleStatus(id, userId);
    }

    @Override
    public void deleteTask(int id, Long userId) {
        taskMapper.deleteById(id, userId);
    }

    @Override
    public void updateTaskMemo(int id, String memo, Long userId) {taskMapper.updateMemo(id, memo, userId);}
}