import os
import sys
from pathlib import Path


def get_project_root():
    """
    获取项目根目录的绝对路径

    Returns:
        str: 项目根目录的绝对路径
    """
    # 方法1: 使用__file__向上查找，适用于从项目内部调用
    current_file = Path(__file__).resolve()
    # 假设utils位于项目根目录的二级子目录下
    project_root = current_file.parent.parent.parent

    return str(project_root)


def get_module_dir():
    """
    获取geometricSum模块目录的绝对路径

    Returns:
        str: geometricSum模块目录的绝对路径
    """
    # 方法1: 从项目根目录推导
    project_root = get_project_root()
    return os.path.join(project_root, "geometricSum")

    # 方法2: 直接使用__file__
    # return os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def get_config_path(config_filename="geometricsum_config.json"):
    """
    获取配置文件的绝对路径

    Args:
        config_filename: 配置文件名称

    Returns:
        str: 配置文件的绝对路径
    """
    module_dir = get_module_dir()
    return os.path.join(module_dir, config_filename)


def add_project_to_path():
    """
    将项目根目录添加到Python路径中，
    这样无论从哪里运行脚本，都能正确导入项目模块
    """
    project_root = get_project_root()
    if project_root not in sys.path:
        sys.path.insert(0, project_root)
        print(f"已将项目根目录添加到Python路径: {project_root}")


# 当作为脚本直接运行时，打印路径信息
if __name__ == "__main__":
    print(f"项目根目录: {get_project_root()}")
    print(f"模块目录: {get_module_dir()}")
    print(f"配置文件路径: {get_config_path()}")

    # 将项目添加到Python路径
    add_project_to_path()

    # 验证导入是否正常工作
    try:
        from python.geometricSum.model.test_case import TestCase

        print("成功导入 TestCase 类")
    except ImportError as e:
        print(f"导入失败: {e}")
