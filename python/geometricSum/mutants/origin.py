'''
计算几何级数的和
'''
def geometric_sum(x):
    """
    计算几何级数的和
    Args:
        x (float): 几何级数的和
    Returns:
        float: 几何级数的和
    """
    n = 0
    s = 0
    while x > s:
        s = s + 1 / (pow(2, n))
        n += 1
    geometric_sum_num = n + 1
    return geometric_sum_num