/**
 * @typedef {Object} Structure
 * @property {[number, number, number]} size - Dimensions [x, y, z]
 * @property {Object.<string, string>} palette - Block palette mapping keys to minecraft block IDs
 * @property {Object.<number, string[][]>} layers - Layer data, keyed by Y level
 */

/**
 * Validates a structure object
 * @param {any} obj - Object to validate
 * @returns {boolean} - Whether the object is a valid structure
 */
export function isValidStructure(obj) {
  if (!obj || typeof obj !== 'object') return false;
  if (!Array.isArray(obj.size) || obj.size.length !== 3) return false;
  if (!obj.palette || typeof obj.palette !== 'object') return false;
  if (!obj.layers || typeof obj.layers !== 'object') return false;
  
  const [sx, sy, sz] = obj.size;
  if (typeof sx !== 'number' || typeof sy !== 'number' || typeof sz !== 'number') return false;
  
  return true;
}

export default { isValidStructure };