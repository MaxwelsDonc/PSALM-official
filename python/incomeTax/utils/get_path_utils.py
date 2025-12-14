"""
Utility module for getting various project-related paths.

This module provides functions to:
- Get the project root directory
- Get the incomeTax module directory
- Get configuration file paths
- Add the project root to Python path
"""

import os
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
    获取incomeTax模块目录的绝对路径

    Returns:
        str: incomeTax模块目录的绝对路径
    """
    # 方法1: 从项目根目录推导
    project_root = get_project_root()
    return os.path.join(project_root, "incomeTax")

    # 方法2: 直接使用__file__
    # return os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def get_config_path(config_filename="incomeTax_config.json"):
    """
    获取配置文件的绝对路径

    Args:
        config_filename: 配置文件名称

    Returns:
        str: 配置文件的绝对路径
    """
    module_dir = get_module_dir()
    return os.path.join(module_dir, config_filename)


# 当作为脚本直接运行时，打印路径信息
if __name__ == "__main__":
    print(f"项目根目录: {get_project_root()}")
    print(f"模块目录: {get_module_dir()}")
    print(f"配置文件路径: {get_config_path()}")
