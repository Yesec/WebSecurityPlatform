// 导航栏增强效果 JavaScript
document.addEventListener('DOMContentLoaded', function() {
    
    // 滚动时导航栏效果
    const navbar = document.querySelector('.navbar');
    if (navbar) {
        window.addEventListener('scroll', function() {
            if (window.scrollY > 50) {
                navbar.classList.add('scrolled');
            } else {
                navbar.classList.remove('scrolled');
            }
        });
    }

    // 导航链接平滑滚动到顶部（如果是首页链接）
    const homeLinks = document.querySelectorAll('a[href="/"], a[href="/#"]');
    homeLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            // 只有在当前页面已经是首页时才阻止默认行为并平滑滚动
            if (window.location.pathname === '/' || window.location.pathname === '/index') {
                e.preventDefault();
                window.scrollTo({
                    top: 0,
                    behavior: 'smooth'
                });
            }
        });
    });

    // 下拉菜单增强动画
    const dropdowns = document.querySelectorAll('.dropdown');
    dropdowns.forEach(dropdown => {
        const toggle = dropdown.querySelector('.dropdown-toggle');
        const menu = dropdown.querySelector('.dropdown-menu');
        
        if (toggle && menu) {
            // 显示下拉菜单时的动画
            dropdown.addEventListener('show.bs.dropdown', function() {
                menu.style.opacity = '0';
                menu.style.transform = 'translateY(-10px)';
                
                setTimeout(() => {
                    menu.style.opacity = '1';
                    menu.style.transform = 'translateY(0)';
                }, 10);
            });
            
            // 隐藏下拉菜单时的动画
            dropdown.addEventListener('hide.bs.dropdown', function() {
                menu.style.opacity = '0';
                menu.style.transform = 'translateY(-10px)';
            });
        }
    });

    // 导航链接点击波纹效果
    const navLinks = document.querySelectorAll('.nav-link');
    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            // 创建波纹效果
            const ripple = document.createElement('span');
            ripple.classList.add('nav-link-ripple');
            
            const rect = this.getBoundingClientRect();
            const size = Math.max(rect.width, rect.height);
            const x = e.clientX - rect.left - size / 2;
            const y = e.clientY - rect.top - size / 2;
            
            ripple.style.cssText = `
                position: absolute;
                border-radius: 50%;
                background: rgba(255, 255, 255, 0.3);
                width: ${size}px;
                height: ${size}px;
                left: ${x}px;
                top: ${y}px;
                pointer-events: none;
                transform: scale(0);
                animation: rippleEffect 0.6s ease-out;
            `;
            
            // 添加波纹动画样式
            if (!document.querySelector('#nav-ripple-styles')) {
                const style = document.createElement('style');
                style.id = 'nav-ripple-styles';
                style.textContent = `
                    @keyframes rippleEffect {
                        to {
                            transform: scale(2);
                            opacity: 0;
                        }
                    }
                `;
                document.head.appendChild(style);
            }
            
            this.appendChild(ripple);
            
            setTimeout(() => {
                ripple.remove();
            }, 600);
        });
    });

    // 品牌logo悬停效果增强
    const brand = document.querySelector('.navbar-brand');
    if (brand) {
        brand.addEventListener('mouseenter', function() {
            this.style.transform = 'translateX(4px) scale(1.05)';
        });
        
        brand.addEventListener('mouseleave', function() {
            this.style.transform = 'translateX(0) scale(1)';
        });
    }

    // 页面加载动画
    const animateOnLoad = () => {
        const elements = document.querySelectorAll('.nav-item, .navbar-auth .btn');
        elements.forEach((el, index) => {
            el.style.opacity = '0';
            el.style.transform = 'translateY(-10px)';
            
            setTimeout(() => {
                el.style.transition = 'all 0.5s ease';
                el.style.opacity = '1';
                el.style.transform = 'translateY(0)';
            }, 100 + index * 50);
        });
    };

    // 如果是页面初次加载，执行动画
    if (performance.navigation.type === 1) {
        // 页面刷新时不执行动画
    } else {
        animateOnLoad();
    }

    // 移动端触摸优化
    if ('ontouchstart' in window) {
        navLinks.forEach(link => {
            link.addEventListener('touchstart', function() {
                this.style.transform = 'scale(0.95)';
            });
            
            link.addEventListener('touchend', function() {
                setTimeout(() => {
                    this.style.transform = '';
                }, 150);
            });
        });
    }

    // 导航栏搜索功能预留（如果未来添加）
    const setupSearch = () => {
        // 这里可以预留搜索功能的JavaScript代码
        console.log('导航栏搜索功能已准备就绪');
    };

    // 初始化搜索功能
    setupSearch();

    // 性能优化：使用节流函数
    function throttle(func, limit) {
        let inThrottle;
        return function() {
            const args = arguments;
            const context = this;
            if (!inThrottle) {
                func.apply(context, args);
                inThrottle = true;
                setTimeout(() => inThrottle = false, limit);
            }
        };
    }

    // 应用节流到滚动事件
    window.addEventListener('scroll', throttle(function() {
        // 滚动相关的性能优化代码
    }, 100));

    // 键盘导航支持
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Tab') {
            // Tab键导航时的视觉反馈
            const focusedElement = document.activeElement;
            if (focusedElement.classList.contains('nav-link')) {
                focusedElement.style.outline = '2px solid var(--primary-color)';
                focusedElement.style.outlineOffset = '2px';
            }
        }
    });

    // 移除焦点outline（当用户点击时）
    document.addEventListener('mousedown', function() {
        document.querySelectorAll('.nav-link').forEach(link => {
            link.style.outline = 'none';
        });
    });

    console.log('导航栏增强效果已加载');
});

// 导出函数供其他脚本使用
window.NavbarEnhancements = {
    // 手动触发滚动效果
    updateNavbarScroll: function() {
        const navbar = document.querySelector('.navbar');
        if (navbar) {
            if (window.scrollY > 50) {
                navbar.classList.add('scrolled');
            } else {
                navbar.classList.remove('scrolled');
            }
        }
    },
    
    // 高亮当前页面导航
    highlightCurrentPage: function() {
        const currentPath = window.location.pathname;
        const navLinks = document.querySelectorAll('.nav-link');
        
        navLinks.forEach(link => {
            link.classList.remove('active');
            const href = link.getAttribute('href');
            
            if (href && currentPath.startsWith(href) && href !== '/') {
                link.classList.add('active');
            } else if (href === '/' && (currentPath === '/' || currentPath === '/index')) {
                link.classList.add('active');
            }
        });
    }
};
