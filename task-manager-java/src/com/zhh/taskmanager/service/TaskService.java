package com.zhh.taskmanager.service; // 在service软件包下
import com.zhh.taskmanager.model.Task; // 引用model包下的Task类
import java.util.List;

public interface TaskService {
    void addTask(Task task);
    List<Task> getTasksByUserId(Long userId);
    void completeTask(int id, Long userIdFromToken);
    void deleteTask(int id, Long userIdFromToken);
    void updateTaskMemo(int id, String memo, Long userIdFromToken);
    // 新增：更新核心信息（标题、优先级、标签）
    void updateTaskCore(int id, String title, String priority, String tag, Long userIdFromToken);
}
