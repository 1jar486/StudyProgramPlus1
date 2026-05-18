package com.zhh.taskmanager.service;

import com.zhh.taskmanager.mapper.ChatMessageMapper;
import com.zhh.taskmanager.mapper.ChatSessionMapper;
import com.zhh.taskmanager.mapper.DocumentMapper;
import com.zhh.taskmanager.mapper.NotebookMapper;
import com.zhh.taskmanager.Entity.Document;
import com.zhh.taskmanager.Entity.Notebook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotebookServiceImpl implements NotebookService {
    @Autowired private NotebookMapper notebookMapper;
    @Autowired private DocumentMapper documentMapper;
    @Autowired private ChatMessageMapper chatMessageMapper;
    @Autowired private ChatSessionMapper chatSessionMapper;
    private final RestTemplate restTemplate = new RestTemplate();

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
    @Transactional // 开启事务，保证数据库的清理一致性
    public void deleteNotebook(Integer id, Long userId) {

        // 1. 先查出挂载的文档，准备物理级销毁
        List<Document> docs = documentMapper.findByNotebookId(userId, id);

        if (docs != null && !docs.isEmpty()) {
            // 【架构级优化】：开启新线程去通知 Python 和删本地文件
            // 这样做的好处是：网络调用绝对不会阻塞 MySQL 的事务，防止 500 崩溃！
            new Thread(() -> {
                for (Document doc : docs) {
                    try {
                        Map<String, Object> pyRequest = new HashMap<>();
                        pyRequest.put("file_path", doc.getFilePath());
                        pyRequest.put("notebook_id", doc.getNotebookId());
                        restTemplate.postForObject("http://localhost:8000/api/ai/delete", pyRequest, String.class);

                        java.io.File physicalFile = new java.io.File(doc.getFilePath());
                        if (physicalFile.exists()) {
                            physicalFile.delete();
                        }
                    } catch (Exception e) {
                        System.err.println("异步清理物理文件或向量失败：" + e.getMessage());
                    }
                }
            }).start();
        }

        // 2. 逻辑级销毁 (严格按顺序清理 MySQL，子表 -> 父表)
        chatMessageMapper.deleteByNotebookId(id);
        chatSessionMapper.deleteByNotebookId(id); // 【新增】抹除孤立的会话
        documentMapper.deleteByNotebookId(id, userId);
        notebookMapper.deleteById(id, userId);
    }
}