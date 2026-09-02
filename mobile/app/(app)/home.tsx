import { useAuth } from '../../context/AuthContext';
import { ClientHome } from '../../components/ClientHome';
import { BarberHome } from '../../components/BarberHome';

export default function HomeScreen() {
  const { session } = useAuth();

  if (!session) return null;

  return session.role === 'CLIENT' ? <ClientHome /> : <BarberHome />;
}
