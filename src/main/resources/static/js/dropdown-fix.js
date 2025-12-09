// 自定义下拉菜单实现 - 修复Bootstrap下拉菜单问题
document.addEventListener('DOMContentLoaded', function() {
    
    // 获取所有下拉菜单元素
    const dropdowns = document.querySelectorAll('.dropdown');
    
    dropdowns.forEach(function(dropdown) {
        const toggle = dropdown.querySelector('.dropdown-toggle');
        const menu = dropdown.querySelector('.dropdown-menu');
        
        if (!toggle || !menu) return;
        
        // 初始化状态
        let isOpen = false;
        
        // 点击切换菜单
        toggle.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            
            if (isOpen) {
                closeDropdown(dropdown, menu, toggle);
            } else {
                // 先关闭其他所有下拉菜单
                closeAllDropdowns();
                openDropdown(dropdown, menu, toggle);
            }
        });
        
        // 菜单项点击后关闭菜单
        const menuItems = menu.querySelectorAll('.dropdown-item');
        menuItems.forEach(function(item) {
            item.addEventListener('click', function() {
                closeDropdown(dropdown, menu, toggle);
            });
        });
    });
    
    // 点击页面其他地方关闭所有下拉菜单
    document.addEventListener('click', function(e) {
        if (!e.target.closest('.dropdown')) {
            closeAllDropdowns();
        }
    });
    
    // 关闭所有下拉菜单的函数
    function closeAllDropdowns() {
        const openDropdowns = document.querySelectorAll('.dropdown.show');
        openDropdowns.forEach(function(dropdown) {
            const toggle = dropdown.querySelector('.dropdown-toggle');
            const menu = dropdown.querySelector('.dropdown-menu');
            if (toggle && menu) {
                closeDropdown(dropdown, menu, toggle);
            }
        });
    }
    
    // 打开下拉菜单的函数
    function openDropdown(dropdown, menu, toggle) {
        dropdown.classList.add('show');
        menu.classList.add('show');
        toggle.setAttribute('aria-expanded', 'true');
        isOpen = true;
        
        // 设置菜单位置
        positionDropdown(menu, toggle);
    }
    
    // 关闭下拉菜单的函数
    function closeDropdown(dropdown, menu, toggle) {
        dropdown.classList.remove('show');
        menu.classList.remove('show');
        toggle.setAttribute('aria-expanded', 'false');
        isOpen = false;
    }
    
    // 定位下拉菜单
    function positionDropdown(menu, toggle) {
        const toggleRect = toggle.getBoundingClientRect();
        
        // 重置位置
        menu.style.position = 'absolute';
        menu.style.top = '';
        menu.style.left = '';
        menu.style.right = '';
        menu.style.bottom = '';
        menu.style.transform = '';
        
        // 检查是否是右对齐的下拉菜单
        if (menu.classList.contains('dropdown-menu-end')) {
            // 右对齐
            menu.style.right = '0';
            menu.style.left = 'auto';
        } else {
            // 左对齐
            menu.style.left = '0';
            menu.style.right = 'auto';
        }
        
        // 设置顶部位置
        menu.style.top = '100%';
        menu.style.marginTop = '0.25rem';
        
        // 检查是否超出屏幕边界
        setTimeout(function() {
            const rect = menu.getBoundingClientRect();
            
            // 如果菜单超出右边界
            if (rect.right > window.innerWidth) {
                menu.style.right = '0';
                menu.style.left = 'auto';
                menu.classList.add('dropdown-menu-end');
            }
            
            // 如果菜单超出左边界
            if (rect.left < 0) {
                menu.style.left = '0';
                menu.style.right = 'auto';
                menu.classList.remove('dropdown-menu-end');
            }
            
            // 如果菜单超出底部边界，向上显示
            if (rect.bottom > window.innerHeight) {
                menu.style.top = 'auto';
                menu.style.bottom = '100%';
                menu.style.marginTop = '0';
                menu.style.marginBottom = '0.25rem';
            }
        }, 0);
    }
    
    // 键盘导航支持
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            closeAllDropdowns();
        }
    });
});

// 导出功能供其他脚本使用
window.DropdownFix = {
    closeAll: function() {
        const openDropdowns = document.querySelectorAll('.dropdown.show');
        openDropdowns.forEach(function(dropdown) {
            const toggle = dropdown.querySelector('.dropdown-toggle');
            const menu = dropdown.querySelector('.dropdown-menu');
            if (toggle && menu) {
                dropdown.classList.remove('show');
                menu.classList.remove('show');
                toggle.setAttribute('aria-expanded', 'false');
            }
        });
    }
};
