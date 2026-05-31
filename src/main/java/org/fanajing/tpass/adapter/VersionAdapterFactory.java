package org.fanajing.tpass.adapter;

/**
 * 版本适配器工厂
 * 根据 Minecraft 版本自动选择对应的适配器实现
 */
public class VersionAdapterFactory {
    
    private static VersionAdapter adapter;
    
    /**
     * 获取当前版本的适配器实例（单例模式）
     * @return VersionAdapter 实例
     */
    public static VersionAdapter getAdapter() {
        if (adapter == null) {
            adapter = createAdapter();
        }
        return adapter;
    }
    
    /**
     * 创建适配器实例
     * 在这里添加对新版本的支持
     * @return VersionAdapter 实例
     */
    private static VersionAdapter createAdapter() {
        // 检测 Minecraft 版本并返回对应的适配器
        // 目前只支持 1.21，后续可以添加更多版本
        
        String mcVersion = getMinecraftVersion();
        
        // 可以根据版本号选择不同的适配器
        if (mcVersion.startsWith("1.21")) {
            return new VersionAdapter_1_21();
        }
        
        // 默认使用 1.21 适配器
        // 未来可以在这里添加更多版本的判断
        // else if (mcVersion.startsWith("1.20")) {
        //     return new VersionAdapter_1_20();
        // }
        
        throw new RuntimeException("不支持的 Minecraft 版本: " + mcVersion);
    }
    
    /**
     * 获取 Minecraft 版本号
     * 从 Forge 或 Fabric API 获取实际版本号
     * @return 版本号字符串
     */
    private static String getMinecraftVersion() {
        // 使用 Forge 的方式获取版本
        try {
            Class<?> sharedConstantsClass = Class.forName("net.minecraft.SharedConstants");
            java.lang.reflect.Method method = sharedConstantsClass.getMethod("getCurrentVersion");
            Object version = method.invoke(null);
            // DetectedVersion 对象调用 getName() 获取版本号
            java.lang.reflect.Method getNameMethod = version.getClass().getMethod("getName");
            return (String) getNameMethod.invoke(version);
        } catch (Exception e) {
            // 如果反射失败，尝试其他方式
            try {
                Class<?> forgeVersionClass = Class.forName("net.minecraftforge.common.ForgeVersion");
                java.lang.reflect.Field mcVersionField = forgeVersionClass.getDeclaredField("mcVersion");
                mcVersionField.setAccessible(true);
                return (String) mcVersionField.get(null);
            } catch (Exception ex) {
                // 最后的备选方案：硬编码默认版本
                System.err.println("无法检测 Minecraft 版本，使用默认版本 1.21");
                return "1.21";
            }
        }
    }
    
    /**
     * 重置适配器（用于测试或热重载）
     */
    public static void reset() {
        adapter = null;
    }
}
