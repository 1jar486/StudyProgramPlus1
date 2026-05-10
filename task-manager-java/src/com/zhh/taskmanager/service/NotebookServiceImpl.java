package com.zhh.taskmanager.service;

import com.zhh.taskmanager.mapper.ChatMessageMapper;
import com.zhh.taskmanager.mapper.DocumentMapper;
import com.zhh.taskmanager.mapper.NotebookMapper;
import com.zhh.taskmanager.model.Notebook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class NotebookServiceImpl implements NotebookService {
    @Autowired private NotebookMapper notebookMapper;
    @Autowired private DocumentMapper documentMapper;
    @Autowired private ChatMessageMapper chatMessageMapper;

    @Override
    public Notebook createNotebook(Long userId, String name) {
        Notebook nb = new Notebook();
        nb.setUserId(userId);
        nb.setName(name);
        notebookMapper.insert(nb);
        return nb;
    }

    @Override
    public List<Notebook> getNotebooks(Long userId) {
        return notebookMapper.findByUserId(userId);
    }

    @Override
    public void renameNotebook(Integer id, String name, Long userId) {
        notebookMapper.updateName(id, name, userId);
    }

    @Override
    @Transactional // 开启事务，保证级联删除的一致性
    public void deleteNotebook(Integer id, Long userId) {
        // 1. 删除笔记本本身的记录
        notebookMapper.deleteById(id, userId);
        // 2. 清理该笔记本下的所有挂载文档
        documentMapper.deleteByNotebookId(id, userId);
        // 3. 清理该笔记本下的所有对话历史
        chatMessageMapper.deleteByNotebookId(id);
    }
}