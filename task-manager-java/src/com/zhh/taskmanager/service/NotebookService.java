package com.zhh.taskmanager.service;
import com.zhh.taskmanager.Entity.Notebook;
import java.util.List;

public interface NotebookService {
    Notebook createNotebook(Long userId, String name);
    List<Notebook> getNotebooks(Long userId);
    void renameNotebook(Integer id, String name, Long userId);
    void deleteNotebook(Integer id, Long userId);
}