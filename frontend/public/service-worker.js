// 空的 Service Worker，用于替换旧的 Service Worker
// 这个文件会被立即注销，不会执行任何操作

// 立即停止并注销自己
self.addEventListener('install', function(event) {
  // 跳过等待，立即激活
  self.skipWaiting();
});

self.addEventListener('activate', function(event) {
  // 立即注销自己
  event.waitUntil(
    self.registration.unregister().then(function() {
      // 清除所有缓存
      return caches.keys().then(function(cacheNames) {
        return Promise.all(
          cacheNames.map(function(cacheName) {
            return caches.delete(cacheName);
          })
        );
      });
    })
  );
});

// 不拦截任何请求
self.addEventListener('fetch', function(event) {
  // 不处理任何请求，直接返回
  return;
});
