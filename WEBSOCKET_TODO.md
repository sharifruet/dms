# WebSocket Implementation TODO

## Current Status

The Document Management System is **fully functional** without WebSocket support. All core features work correctly:

✅ Authentication & Authorization
✅ Document Management (Upload, Download, View, Delete)
✅ Notifications (via REST API with 30-second polling)
✅ Search & Filters
✅ Expiry Tracking
✅ Workflows
✅ Document Versioning
✅ System Health Monitoring
✅ User Management

## WebSocket Implementation (Future Enhancement)

WebSocket support for real-time updates is currently **not implemented** on the backend. The frontend has been configured to gracefully degrade:

- **Notifications**: Poll every 30 seconds via REST API instead of real-time WebSocket
- **Activity Feed**: Shows mock data instead of live activity stream
- **User Presence**: Shows mock presence data

### What's Needed for Full WebSocket Support

To enable real-time WebSocket features, the backend needs:

#### 1. Add WebSocket Dependencies

Add to `backend/pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
<dependency>
    <groupId>org.webjars</groupId>
    <artifactId>sockjs-client</artifactId>
    <version>1.5.1</version>
</dependency>
<dependency>
    <groupId>org.webjars</groupId>
    <artifactId>stomp-websocket</artifactId>
    <version>2.3.4</version>
</dependency>
```

#### 2. Create WebSocket Configuration

Create `backend/src/main/java/com/bpdb/dms/config/WebSocketConfig.java`:

```java
package com.bpdb.dms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/user");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

#### 3. Create WebSocket Controllers

Example notification controller:

```java
package com.bpdb.dms.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketNotificationController {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    public WebSocketNotificationController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    
    // Send notification to specific user
    public void sendNotificationToUser(Long userId, Notification notification) {
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/notifications",
            notification
        );
    }
    
    // Broadcast activity to all users
    public void broadcastActivity(Activity activity) {
        messagingTemplate.convertAndSend("/topic/activity", activity);
    }
    
    // Broadcast user presence
    public void broadcastPresence(UserPresence presence) {
        messagingTemplate.convertAndSend("/topic/user-presence", presence);
    }
}
```

#### 4. Update Notification Service

Modify `NotificationService` to send WebSocket messages when creating notifications:

```java
@Autowired
private WebSocketNotificationController wsController;

public Notification createNotification(NotificationRequest request) {
    Notification notification = // ... create notification
    notificationRepository.save(notification);
    
    // Send real-time WebSocket update
    wsController.sendNotificationToUser(
        notification.getUser().getId(),
        notification
    );
    
    return notification;
}
```

#### 5. Enable WebSocket in Frontend

Uncomment the WebSocket code in:
- `frontend/src/components/NotificationBell.tsx` (line 52-53)
- `frontend/src/components/ActivityFeed.tsx` (lines 62-66, 121-153)

### Testing WebSocket

1. Start backend with WebSocket configuration
2. Login to frontend
3. Open browser console and check for:
   ```
   [WebSocket Debug] Connected
   ```
4. Create a notification from another tab/user
5. Should see real-time notification appear without page refresh

## Priority

WebSocket is a **nice-to-have enhancement** but not critical for production use. The current REST API polling works well for most use cases.

Consider implementing WebSocket if:
- Users require instant notifications (< 5 seconds)
- High-frequency updates are needed (e.g., live collaboration)
- User presence tracking is important

## Alternative: Server-Sent Events (SSE)

If WebSocket proves complex, consider Server-Sent Events (SSE) for one-way real-time updates from server to client. SSE is simpler to implement and works well for notifications.

