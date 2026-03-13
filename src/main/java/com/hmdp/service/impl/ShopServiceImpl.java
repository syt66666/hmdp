package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {


    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;
    @Override
    public Result queryById(Long id) {

        //用互斥锁解决缓存击穿
        Shop shop = cacheClient
                .queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //返回
        return Result.ok(shop);

    }

//    private Shop queryWithLogicalExpire(Long id) {
//        String key = CACHE_SHOP_KEY + id;
//        //1.从redis查询缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        //2.判断缓存是否存在
//        if (StrUtil.isBlank(shopJson)) {
//            //3.未命中 返回为空
//            return null;
//        }
//        //命中，需要把json反序列化为对象
//        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
//        Shop shop = JSONUtil.toBean((JSONObject)redisData.getData(), Shop.class);
//        LocalDateTime expireTime = redisData.getExpireTime();
//        //5.判断是否过期
//        if (expireTime.isAfter(LocalDateTime.now())){
//            //5.1.未过期，直接返回店铺消息
//            return shop;
//        }
//        //5.2.过期，需要缓存重建
//        //6.1.缓存重建
//        //6.2.获取互斥锁
//        String lockKey = LOCK_SHOP_KEY + id;
//        if (tryLock(lockKey)) {
//            //6.3.获取锁成功，开启独立线程，实现缓存重建
//            CACHE_REBUILD_EXECUTOR.submit(() -> {
//                //6.2.重建缓存
//                saveShop2Redis(id,20L);
//                //6.5.释放锁
//                unLock(lockKey);
//            });
//        }
//        //6.4.失败，返回店铺信息
//        return shop;
//    }
//    public Shop queryWithMutex(Long id) {
//        //1.从redis查询缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
//        //2.判断缓存是否存在
//        if (StrUtil.isNotBlank(shopJson)) {
//            //3。存在，直接返回
//            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
//            log.info("缓存存在，{}",shop);
//            return shop;
//        }
//        if(shopJson != null){
//            //缓存命中并且命中的为空值
//            return null;
//        }
//        Shop shop = null;
//        //4.实现缓存重建
//        //4.1.获取互斥锁
//        String lockKey = LOCK_SHOP_KEY + id;
//        try {
//
//            boolean isLock = tryLock(lockKey);
//            //4.2.判断是否获取成功
//            if (!isLock){
//                //4.4.失败，则休眠并重试
//                Thread.sleep(50);
//                queryWithMutex(id);
//            }
//            //4.3.成功，根据id查询数据库
//            shop = getById(id);
//            //5.数据库不存在，返回错误
//            if(shop == null)
//            {
//                // 将空值写入redis
//                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "",CACHE_NULL_TTL, TimeUnit.MINUTES);
//                return null;
//            }
//            //6.存在，写入缓存，返回数据
//            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
//
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }finally {
//            //7.释放互斥锁
//            unLock(lockKey);
//        }
//        return shop;
//    }

//    public Shop queryWithPassThrough(Long id) {
//        //1.从redis查询缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
//        //2.判断缓存是否存在
//        if (StrUtil.isNotBlank(shopJson)) {
//            //3。存在，直接返回
//            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
//            log.info("缓存存在，{}",shop);
//            return shop;
//        }
//        if(shopJson != null){
//            //缓存命中并且命中的为空值
//            return null;
//        }
//        //4.不存在，根据id查询数据库
//        Shop shop = getById(id);
//        log.info("缓存不存在，{}",shop);
//        //5.数据库不存在，返回错误
//        if(shop == null)
//        {
//            // 将空值写入redis
//            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "",CACHE_NULL_TTL, TimeUnit.MINUTES);
//            return null;
//        }
//        //6.存在，写入缓存，返回数据
//        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        return shop;
//    }
//    private boolean tryLock(String key) {
//        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
//        return BooleanUtil.isTrue(flag);
//    }
//    private void unLock(String key) {
//        stringRedisTemplate.delete(key);
//    }
//    private void saveShop2Redis(Long id,Long expireSeconds) {
//        //1.查询店铺数据
//        Shop shop = getById(id);
//        try {
//            Thread.sleep(200);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//        //2.封装逻辑过期时间
//        RedisData redisData = new RedisData();
//        redisData.setData(shop);
//        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
//        //3.写入redis
//        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
//    }
    @Override
    @Transactional
    public Object update(Shop shop) {
        Long id = shop.getId();
        if(id == null){
            return Result.fail("店铺id不能为空");
        }
        //1.更新数据库
        updateById(shop);
        //2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return null;
    }
}
